package com.inuteamflow.server.domain.intip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
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
import org.mockito.InOrder;
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

    @Test
    @DisplayName("API가 최신순으로 주더라도 오래된 공지부터 처리해 커서를 가장 최신 공지에 둔다")
    void processesOldestFirstAndAdvancesCursorToNewest() {
        IntipNoticeResponse newest = notice(103L);
        IntipNoticeResponse middle = notice(102L);
        IntipNoticeResponse oldest = notice(101L);
        AiClassificationResult irrelevant = mock(AiClassificationResult.class);
        when(irrelevant.isRelevant()).thenReturn(false);
        when(intipClient.fetchNoticesWithContent(1)).thenReturn(new ArrayList<>(List.of(newest, middle, oldest)));
        when(aiClassificationClient.classify(any())).thenReturn(irrelevant);

        intipSyncService.sync();

        InOrder inOrder = inOrder(aiClassificationClient);
        inOrder.verify(aiClassificationClient).classify(oldest);
        inOrder.verify(aiClassificationClient).classify(middle);
        inOrder.verify(aiClassificationClient).classify(newest);
        assertThat(cursor.getLastProcessedNoticeId()).isEqualTo(103L);
    }

    @Test
    @DisplayName("API가 최신순으로 줄 때 중간에 AI 장애가 나면 커서를 처리한 가장 최신 공지에 두고 이후 공지는 처리하지 않는다")
    void keepsCursorAtProcessedNoticeWhenAiFailsMidwayWithNewestFirstResponse() {
        IntipNoticeResponse newest = notice(103L);
        IntipNoticeResponse middle = notice(102L);
        IntipNoticeResponse oldest = notice(101L);
        AiClassificationResult irrelevant = mock(AiClassificationResult.class);
        when(irrelevant.isRelevant()).thenReturn(false);
        when(intipClient.fetchNoticesWithContent(1)).thenReturn(new ArrayList<>(List.of(newest, middle, oldest)));
        lenient().when(aiClassificationClient.classify(oldest)).thenReturn(irrelevant);
        lenient()
                .when(aiClassificationClient.classify(middle))
                .thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE));
        lenient().when(aiClassificationClient.classify(newest)).thenReturn(irrelevant);

        intipSyncService.sync();

        assertThat(cursor.getLastProcessedNoticeId()).isEqualTo(101L); // 다음 주기에 102부터 다시 처리
        verify(aiClassificationClient, never()).classify(newest);
    }

    private IntipNoticeResponse notice(Long id) {
        IntipNoticeResponse notice = mock(IntipNoticeResponse.class);
        when(notice.getId()).thenReturn(id);
        return notice;
    }
}
