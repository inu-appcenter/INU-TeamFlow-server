package com.inuteamflow.server.domain.intip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.inuteamflow.server.domain.infoPost.entity.InfoPost;
import com.inuteamflow.server.domain.infoPost.repository.InfoPostRepository;
import com.inuteamflow.server.domain.intip.client.AiClassificationClient;
import com.inuteamflow.server.domain.intip.client.IntipClient;
import com.inuteamflow.server.domain.intip.dto.AiClassificationResult;
import com.inuteamflow.server.domain.intip.dto.IntipNoticeResponse;
import com.inuteamflow.server.domain.intip.entity.IntipSyncCursor;
import com.inuteamflow.server.domain.intip.repository.IntipSyncCursorRepository;
import com.inuteamflow.server.domain.intip.service.IntipSyncService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * {@link IntipSyncService}가 AI 호출 실패 종류에 따라 커서를 올리거나 유지하는지 Mockito 기반 단위 테스트로 검증한다.
 * - 일시적 장애(타임아웃, 5xx 등)면 동기화를 멈추고 그 공지의 커서를 올리지 않는다.
 * - 공지 자체의 문제(400)면 그 공지만 건너뛰고 커서를 올린다.
 */
@ExtendWith(MockitoExtension.class)
class IntipSyncServiceFailureTest {

    @InjectMocks
    private IntipSyncService intipSyncService;

    @Mock
    private IntipClient intipClient;

    @Mock
    private AiClassificationClient aiClassificationClient;

    @Mock
    private IntipSyncCursorRepository cursorRepository;

    @Mock
    private InfoPostRepository infoPostRepository;

    @Spy
    private TransactionTemplate transactionTemplate = new TransactionTemplate(mock(PlatformTransactionManager.class));

    private IntipSyncCursor cursor;

    @BeforeEach
    void setUp() {
        cursor = new IntipSyncCursor();
        when(cursorRepository.findById(IntipSyncCursor.SINGLETON_ID)).thenReturn(Optional.of(cursor));
    }

    @Test
    @DisplayName("AI 서버가 응답하지 않으면 동기화를 멈추고 커서를 올리지 않는다")
    void stopsWithoutAdvancingCursorWhenAiTimesOut() {
        IntipNoticeResponse first = notice(101L);
        when(intipClient.fetchNoticesWithContent(1)).thenReturn(new ArrayList<>(List.of(first)));
        when(aiClassificationClient.classify(first)).thenThrow(new ResourceAccessException("timeout"));

        intipSyncService.sync();

        assertThat(cursor.getLastProcessedNoticeId()).isNull();
        verify(infoPostRepository, never()).save(any(InfoPost.class));
    }

    @Test
    @DisplayName("두 번째 공지에서 AI 장애가 나면 첫 번째 공지까지만 처리하고 커서는 첫 번째 공지에 둔다")
    void keepsCursorAtLastProcessedNoticeWhenAiFailsMidway() {
        IntipNoticeResponse first = notice(101L);
        IntipNoticeResponse second = notice(102L);
        AiClassificationResult irrelevant = mock(AiClassificationResult.class);
        when(irrelevant.isRelevant()).thenReturn(false);
        when(intipClient.fetchNoticesWithContent(1)).thenReturn(new ArrayList<>(List.of(first, second)));
        when(aiClassificationClient.classify(first)).thenReturn(irrelevant);
        when(aiClassificationClient.classify(second))
                .thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE));

        intipSyncService.sync();

        assertThat(cursor.getLastProcessedNoticeId()).isEqualTo(101L);
    }

    @Test
    @DisplayName("AI가 400으로 거부한 공지는 건너뛰고 커서를 올린다")
    void skipsNoticeAndAdvancesCursorWhenAiRejectsWithBadRequest() {
        IntipNoticeResponse first = notice(101L);
        when(intipClient.fetchNoticesWithContent(1)).thenReturn(new ArrayList<>(List.of(first)));
        when(aiClassificationClient.classify(first))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY, new byte[0], null));

        intipSyncService.sync();

        assertThat(cursor.getLastProcessedNoticeId()).isEqualTo(101L);
        verify(infoPostRepository, never()).save(any(InfoPost.class));
    }

    private IntipNoticeResponse notice(Long id) {
        IntipNoticeResponse notice = mock(IntipNoticeResponse.class);
        when(notice.getId()).thenReturn(id);
        return notice;
    }
}
