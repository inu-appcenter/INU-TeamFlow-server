package com.inuteamflow.server.domain.intip.service;

import com.inuteamflow.server.domain.infoPost.entity.InfoPost;
import com.inuteamflow.server.domain.infoPost.enums.InfoPostCategory;
import com.inuteamflow.server.domain.infoPost.enums.InfoPostType;
import com.inuteamflow.server.domain.infoPost.repository.InfoPostRepository;
import com.inuteamflow.server.domain.intip.client.AiClassificationClient;
import com.inuteamflow.server.domain.intip.client.AiClassificationParseException;
import com.inuteamflow.server.domain.intip.client.IntipClient;
import com.inuteamflow.server.domain.intip.dto.AiClassificationResult;
import com.inuteamflow.server.domain.intip.dto.IntipNoticeResponse;
import com.inuteamflow.server.domain.intip.entity.IntipSyncCursor;
import com.inuteamflow.server.domain.intip.repository.IntipSyncCursorRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntipSyncService {

    private static final int MAX_PAGE_SCAN = 10; // 커서를 못 찾고 무한정 페이지를 넘기지 않도록 상한

    private final IntipClient intipClient;
    private final AiClassificationClient aiClassificationClient;
    private final IntipSyncCursorRepository cursorRepository;
    private final InfoPostRepository infoPostRepository;
    private final TransactionTemplate transactionTemplate;

    public void sync() {
        IntipSyncCursor cursor = cursorRepository
                .findById(IntipSyncCursor.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("intip_sync_cursor 초기 행이 없음"));

        List<IntipNoticeResponse> newNotices = collectNewNotices(cursor.getLastProcessedNoticeId());
        if (newNotices.isEmpty()) {
            log.info("[INTIP 동기화] 신규 공지 없음");
            return;
        }

        // 오래된 것부터 처리해야 커서를 순서대로 안전하게 갱신할 수 있음
        newNotices.sort(Comparator.comparing(IntipNoticeResponse::getId));

        for (IntipNoticeResponse notice : newNotices) {
            InfoPost infoPost = classifyNotice(notice); // 외부 호출은 트랜잭션 밖에서 처리, 저장 대상이 아니면 null
            saveAndAdvanceCursor(infoPost, notice.getId()); // 저장과 커서 갱신만 짧은 트랜잭션으로 처리
        }

        log.info(
                "[INTIP 동기화] {}건 처리, 커서 갱신 id={}",
                newNotices.size(),
                newNotices.get(newNotices.size() - 1).getId());
    }

    private List<IntipNoticeResponse> collectNewNotices(Long lastProcessedId) {
        List<IntipNoticeResponse> result = new ArrayList<>();

        for (int page = 1; page <= MAX_PAGE_SCAN; page++) {
            List<IntipNoticeResponse> pageNotices = intipClient.fetchNoticesWithContent(page);
            if (pageNotices.isEmpty()) {
                break;
            }

            // 최초 실행(커서 없음)이면 과거 전체를 끌어오지 않고 1페이지만 보고 추적을 시작함
            if (lastProcessedId == null) {
                return pageNotices;
            }

            boolean reachedCursor = false;
            for (IntipNoticeResponse notice : pageNotices) {
                if (notice.getId().equals(lastProcessedId)) {
                    reachedCursor = true;
                    break;
                }
                result.add(notice);
            }

            if (reachedCursor) {
                break;
            }
        }

        return result;
    }

    private InfoPost classifyNotice(IntipNoticeResponse notice) {
        AiClassificationResult result;
        try {
            result = aiClassificationClient.classify(notice);
        } catch (AiClassificationParseException e) {
            log.warn("[INTIP 동기화] 분류 응답 파싱 실패, notice id={}", notice.getId(), e);
            return null; // 스킵 (재시도 대상 아님)
        } catch (Exception e) {
            log.warn("[INTIP 동기화] AI 서버 호출 실패, notice id={}", notice.getId(), e);
            return null; // 스킵 (별도 재처리 테이블은 아직 없음 - 필요하면 추가)
        }

        if (!result.isRelevant()) {
            return null;
        }

        InfoPostCategory category = toValidCategory(result.getCategory());
        if (category == null) {
            log.warn(
                    "[INTIP 동기화] AI가 유효하지 않은 카테고리 반환, notice id={}, category={}", notice.getId(), result.getCategory());
            return null;
        }

        return InfoPost.createFromIntip(category, notice.getTitle(), formatContent(notice), notice.getUrl());
    }

    /**
     * 정보글 저장과 커서 갱신을 하나의 짧은 트랜잭션으로 처리한다.
     *
     * <p>저장 대상이 아닌 공지({@code infoPost}가 null)도 커서는 갱신한다. 저장과 커서 갱신을 함께 커밋해,
     * 저장만 되고 커서가 갱신되지 않아 같은 공지가 중복 등록되는 일을 막는다.</p>
     *
     * @param infoPost 저장할 정보글, 저장 대상이 아니면 {@code null}
     * @param noticeId 처리한 공지 ID
     */
    private void saveAndAdvanceCursor(InfoPost infoPost, Long noticeId) {
        transactionTemplate.executeWithoutResult(status -> {
            if (infoPost != null) {
                infoPost.assignAuditor(InfoPost.SYSTEM_AUTHOR_ID); // 스케줄러 컨텍스트엔 로그인 사용자가 없어 예약 ID 로 지정
                infoPostRepository.save(infoPost);
            }

            // 이 트랜잭션의 영속성 컨텍스트에서 다시 조회해야 커서 변경이 커밋 때 저장된다
            IntipSyncCursor cursor = cursorRepository
                    .findById(IntipSyncCursor.SINGLETON_ID)
                    .orElseThrow(() -> new IllegalStateException("intip_sync_cursor 초기 행이 없음"));
            cursor.updateCursor(noticeId);
        });
    }

    /**
     * 정규식 포매터로 개행을 복원한 본문을 AI로 한 번 더 정리한다.
     *
     * <p>AI 호출이 실패하거나 결과가 원문과 다른 글자를 포함하면 정규식 포매터 결과를 그대로 쓴다.
     */
    private String formatContent(IntipNoticeResponse notice) {
        String formatted = IntipContentFormatter.format(notice.getContentText());
        if (formatted == null || formatted.isBlank()) {
            return formatted;
        }

        try {
            String aiFormatted = aiClassificationClient.formatContent(notice.getId(), formatted);
            if (IntipContentFormatter.hasSameText(formatted, aiFormatted)) {
                return IntipContentFormatter.removeRedundantAddedSymbols(aiFormatted.strip());
            }
            log.warn("[INTIP 동기화] AI 본문 정리 결과가 원문과 달라 정규식 결과 사용, notice id={}", notice.getId());
        } catch (Exception e) {
            log.warn("[INTIP 동기화] AI 본문 정리 실패, 정규식 결과 사용, notice id={}", notice.getId(), e);
        }
        return formatted;
    }

    private InfoPostCategory toValidCategory(String category) {
        if (category == null) {
            return null;
        }
        try {
            InfoPostCategory parsed = InfoPostCategory.valueOf(category);
            return parsed.getType() == InfoPostType.NOTICE ? parsed : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
