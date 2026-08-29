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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntipSyncService {

    private static final int MAX_PAGE_SCAN = 10; // 커서를 못 찾고 무한정 페이지를 넘기지 않도록 상한

    private final IntipClient intipClient;
    private final AiClassificationClient aiClassificationClient;
    private final IntipSyncCursorRepository cursorRepository;
    private final InfoPostRepository infoPostRepository;

    @Value("${moimi.system-user-id}")
    private Long systemUserId;

    @Transactional
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

        Long latestProcessedId = cursor.getLastProcessedNoticeId();
        for (IntipNoticeResponse notice : newNotices) {
            classifyAndSave(notice);
            latestProcessedId = notice.getId();
        }

        cursor.updateCursor(latestProcessedId);
        log.info("[INTIP 동기화] {}건 처리, 커서 갱신 id={}", newNotices.size(), latestProcessedId);
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

    private void classifyAndSave(IntipNoticeResponse notice) {
        AiClassificationResult result;
        try {
            result = aiClassificationClient.classify(notice);
        } catch (AiClassificationParseException e) {
            log.warn("[INTIP 동기화] 분류 응답 파싱 실패, notice id={}", notice.getId(), e);
            return; // 스킵 (재시도 대상 아님)
        } catch (Exception e) {
            log.warn("[INTIP 동기화] AI 서버 호출 실패, notice id={}", notice.getId(), e);
            return; // 스킵 (별도 재처리 테이블은 아직 없음 - 필요하면 추가)
        }

        if (!result.isRelevant()) {
            return;
        }

        InfoPostCategory category = toValidCategory(result.getCategory());
        if (category == null) {
            log.warn(
                    "[INTIP 동기화] AI가 유효하지 않은 카테고리 반환, notice id={}, category={}", notice.getId(), result.getCategory());
            return;
        }

        InfoPost infoPost = InfoPost.create(category, notice.getTitle(), notice.getContentText());
        infoPost.assignAuditor(systemUserId); // 스케줄러 컨텍스트엔 로그인 사용자가 없어 수동 지정
        infoPostRepository.save(infoPost);
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
