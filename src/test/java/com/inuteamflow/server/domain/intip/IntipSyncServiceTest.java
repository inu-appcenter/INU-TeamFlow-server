package com.inuteamflow.server.domain.intip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

/**
 * {@link IntipSyncService}가 AI 본문 정리 결과를 원문과 대조해, 통과하면 AI 결과를, 아니면 정규식 포매터 결과를 저장하는지
 * Mockito 기반 단위 테스트로 검증한다.
 * - Spring Context, 실제 데이터베이스, INTIP/AI 서버를 사용하지 않는다.
 */
@ExtendWith(MockitoExtension.class)
class IntipSyncServiceTest {

    private static final Long NOTICE_ID = 100L;
    private static final String CONTENT = "모집 안내 - 모집인원 : 30명 - 모집기간 : ~ 11. 13.(금)";
    private static final String REGEX_FORMATTED = "모집 안내\n- 모집인원 : 30명\n- 모집기간 : ~ 11. 13.(금)";

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

    @BeforeEach
    void setUp() {
        IntipNoticeResponse notice = mock(IntipNoticeResponse.class);
        when(notice.getId()).thenReturn(NOTICE_ID);
        when(notice.getTitle()).thenReturn("모집 공지");
        when(notice.getContentText()).thenReturn(CONTENT);
        when(notice.getUrl()).thenReturn("https://www.inu.ac.kr/notice");

        AiClassificationResult result = mock(AiClassificationResult.class);
        when(result.isRelevant()).thenReturn(true);
        when(result.getCategory()).thenReturn("CONTEST");

        when(cursorRepository.findById(IntipSyncCursor.SINGLETON_ID)).thenReturn(Optional.of(new IntipSyncCursor()));
        when(intipClient.fetchNoticesWithContent(1)).thenReturn(new ArrayList<>(List.of(notice)));
        when(aiClassificationClient.classify(notice)).thenReturn(result);
        when(infoPostRepository.save(any(InfoPost.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("AI 정리 결과가 원문과 같은 글자면 겹친 기호만 지우고 AI 결과를 저장한다")
    void savesAiFormattedContent() {
        String aiFormatted = "■ 모집 안내\n\n• - 모집인원 : 30명\n- 모집기간 : ~ 11. 13.(금)\n";
        when(aiClassificationClient.formatContent(NOTICE_ID, REGEX_FORMATTED)).thenReturn(aiFormatted);

        intipSyncService.sync();

        assertThat(savedContent()).isEqualTo("■ 모집 안내\n\n- 모집인원 : 30명\n- 모집기간 : ~ 11. 13.(금)");
    }

    @Test
    @DisplayName("AI 정리 결과에서 글자가 바뀌면 정규식 포매터 결과를 저장한다")
    void savesRegexFormattedContentWhenTextChanged() {
        when(aiClassificationClient.formatContent(eq(NOTICE_ID), anyString()))
                .thenReturn("■ 모집 안내\n- 모집인원 : 31명\n- 모집기간 : ~ 11. 13.(금)");

        intipSyncService.sync();

        assertThat(savedContent()).isEqualTo(REGEX_FORMATTED);
    }

    @Test
    @DisplayName("AI 정리 호출이 실패하면 정규식 포매터 결과를 저장한다")
    void savesRegexFormattedContentWhenAiFails() {
        when(aiClassificationClient.formatContent(eq(NOTICE_ID), anyString()))
                .thenThrow(new RestClientException("AI 서버 오류"));

        intipSyncService.sync();

        assertThat(savedContent()).isEqualTo(REGEX_FORMATTED);
    }

    @Test
    @DisplayName("AI 정리 응답이 비어 있으면 정규식 포매터 결과를 저장한다")
    void savesRegexFormattedContentWhenAiReturnsNull() {
        when(aiClassificationClient.formatContent(eq(NOTICE_ID), anyString())).thenReturn(null);

        intipSyncService.sync();

        assertThat(savedContent()).isEqualTo(REGEX_FORMATTED);
    }

    private String savedContent() {
        ArgumentCaptor<InfoPost> captor = ArgumentCaptor.forClass(InfoPost.class);
        verify(infoPostRepository).save(captor.capture());
        return captor.getValue().getContent();
    }
}
