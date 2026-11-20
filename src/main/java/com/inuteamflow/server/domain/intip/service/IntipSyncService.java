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
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

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

        int processedCount = 0;
        for (IntipNoticeResponse notice : newNotices) {
            InfoPost infoPost;
            try {
                infoPost = classifyNotice(notice); // 외부 호출은 트랜잭션 밖에서 처리, 저장 대상이 아니면 null
            } catch (RestClientException e) {
                // AI 서버 쪽 일시적 장애: 이 공지를 처리 완료로 기록하지 않고 멈춘다. 다음 주기에 이 공지부터 다시 처리한다
                log.error(
                        "[INTIP 동기화] AI 서버 장애로 중단, 다음 주기에 notice id={}부터 재시도 ({}/{}건 처리)",
                        notice.getId(),
                        processedCount,
                        newNotices.size(),
                        e);
                return;
            }
            saveAndAdvanceCursor(infoPost, notice.getId()); // 저장과 커서 갱신만 짧은 트랜잭션으로 처리
            processedCount++;
        }

        log.info(
                "[INTIP 동기화] {}건 처리, 커서 갱신 id={}",
                processedCount,
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
            log.warn("[INTIP 동기화] 분류 응답 파싱 실패, 해당 공지만 건너뜀, notice id={}", notice.getId(), e);
            return null; // 이 공지 자체의 문제라 재시도해도 같은 결과
        } catch (HttpClientErrorException.BadRequest e) {
            log.warn("[INTIP 동기화] AI가 요청을 거부(400), 해당 공지만 건너뜀, notice id={}", notice.getId(), e);
            return null; // 이 공지 자체의 문제
        } catch (RestClientException e) {
            throw e; // 타임아웃, 5xx, 401, 429 등 일시적/시스템 문제는 호출부(sync)에서 동기화를 멈춘다
        } catch (Exception e) {
            log.error("[INTIP 동기화] 예상하지 못한 오류, 해당 공지만 건너뜀, notice id={}", notice.getId(), e);
            return null; // 한 공지의 알 수 없는 오류가 뒤의 공지를 막지 않게 건너뜀
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
