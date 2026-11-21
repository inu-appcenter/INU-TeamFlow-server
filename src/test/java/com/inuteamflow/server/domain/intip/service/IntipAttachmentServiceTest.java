package com.inuteamflow.server.domain.intip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.inuteamflow.server.domain.infoPost.entity.InfoPost;
import com.inuteamflow.server.domain.infoPost.entity.InfoPostAttachment;
import com.inuteamflow.server.domain.infoPost.entity.InfoPostImage;
import com.inuteamflow.server.domain.infoPost.enums.InfoPostCategory;
import com.inuteamflow.server.domain.infoPost.repository.InfoPostAttachmentRepository;
import com.inuteamflow.server.domain.infoPost.repository.InfoPostImageRepository;
import com.inuteamflow.server.domain.intip.client.IntipClient;
import com.inuteamflow.server.domain.intip.dto.IntipNoticeResponse.Attachment;
import com.inuteamflow.server.domain.intip.service.IntipAttachmentService.PreparedAttachments;
import com.inuteamflow.server.global.s3.S3Service;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

/**
 * {@link IntipAttachmentService}가 이미지 첨부는 S3에 올려 정보글 이미지로, 나머지는 다운로드 URL만 첨부파일로 저장하는지
 * Mockito 기반 단위 테스트로 검증한다.
 * - Spring Context, 실제 데이터베이스, 학교 서버, S3를 사용하지 않는다.
 */
@ExtendWith(MockitoExtension.class)
class IntipAttachmentServiceTest {

    private final InfoPost infoPost = InfoPost.createFromIntip(InfoPostCategory.CONTEST, "공지", "본문", "https://inu");

    @InjectMocks
    private IntipAttachmentService intipAttachmentService;

    @Mock
    private IntipClient intipClient;

    @Mock
    private S3Service s3Service;

    @Mock
    private InfoPostImageRepository infoPostImageRepository;

    @Mock
    private InfoPostAttachmentRepository infoPostAttachmentRepository;

    @Captor
    private ArgumentCaptor<List<InfoPostImage>> imagesCaptor;

    @Captor
    private ArgumentCaptor<List<InfoPostAttachment>> filesCaptor;

    @Test
    @DisplayName("이미지는 확장자에 맞는 Content-Type으로 S3에 올리고, 나머지는 첨부파일로 나눈다")
    void prepareSplitsImagesAndFiles() {
        byte[] jpg = {1, 2};
        byte[] png = {3, 4};
        when(intipClient.downloadFile("https://inu/poster.jpg")).thenReturn(jpg);
        when(intipClient.downloadFile("https://inu/card.png")).thenReturn(png);
        when(s3Service.uploadInfoPostImage("포스터.jpg", "image/jpeg", jpg)).thenReturn("info-posts/image/a.jpg");
        when(s3Service.uploadInfoPostImage("카드뉴스.PNG", "image/png", png)).thenReturn("info-posts/image/b.png");
        Attachment hwp = attachment("붙임1. 계획서.hwp", "https://inu/plan.hwp", "hwp");
        Attachment pdf = attachment("안내문.pdf", "https://inu/guide.pdf", "PDF");

        PreparedAttachments prepared = intipAttachmentService.prepare(List.of(
                hwp,
                attachment("포스터.jpg", "https://inu/poster.jpg", "jpg"),
                pdf,
                attachment("카드뉴스.PNG", "https://inu/card.png", "PNG")));

        assertThat(prepared.imageKeys()).containsExactly("info-posts/image/a.jpg", "info-posts/image/b.png");
        assertThat(prepared.files()).containsExactly(hwp, pdf);
    }

    @Test
    @DisplayName("이미지 다운로드에 실패하면 첨부파일로 돌린다")
    void prepareMovesImageToFilesWhenDownloadFails() {
        Attachment jpg = attachment("포스터.jpg", "https://inu/poster.jpg", "jpg");
        when(intipClient.downloadFile("https://inu/poster.jpg")).thenThrow(new RestClientException("학교 서버 오류"));

        PreparedAttachments prepared = intipAttachmentService.prepare(List.of(jpg));

        assertThat(prepared.imageKeys()).isEmpty();
        assertThat(prepared.files()).containsExactly(jpg);
    }

    @Test
    @DisplayName("이름이나 URL이 없는 첨부는 건너뛴다")
    void prepareSkipsAttachmentWithoutUrl() {
        PreparedAttachments prepared = intipAttachmentService.prepare(List.of(attachment("포스터.jpg", " ", "jpg")));

        assertThat(prepared.imageKeys()).isEmpty();
        assertThat(prepared.files()).isEmpty();
        verify(s3Service, never()).uploadInfoPostImage(any(), any(), any());
    }

    @Test
    @DisplayName("첨부파일이 없으면 외부 호출 없이 빈 결과를 돌려준다")
    void prepareReturnsEmptyWhenNoAttachments() {
        assertThat(intipAttachmentService.prepare(null)).isEqualTo(PreparedAttachments.EMPTY);
        assertThat(intipAttachmentService.prepare(List.of())).isEqualTo(PreparedAttachments.EMPTY);

        verifyNoInteractions(intipClient, s3Service);
    }

    @Test
    @DisplayName("준비한 이미지와 첨부파일을 게시된 순서대로 정보글에 저장한다")
    void saveStoresImagesAndFilesInOrder() {
        PreparedAttachments prepared = new PreparedAttachments(
                List.of("info-posts/image/a.jpg", "info-posts/image/b.png"),
                List.of(
                        attachment("붙임1. 계획서.hwp", "https://inu/plan.hwp", "hwp"),
                        attachment("안내문.pdf", "https://inu/guide.pdf", "PDF")));

        intipAttachmentService.save(infoPost, prepared);

        verify(infoPostImageRepository).saveAll(imagesCaptor.capture());
        assertThat(imagesCaptor.getValue())
                .extracting(InfoPostImage::getImageKey, InfoPostImage::getSortOrder, InfoPostImage::getInfoPost)
                .containsExactly(
                        tuple("info-posts/image/a.jpg", 0, infoPost), tuple("info-posts/image/b.png", 1, infoPost));

        verify(infoPostAttachmentRepository).saveAll(filesCaptor.capture());
        assertThat(filesCaptor.getValue())
                .extracting(
                        InfoPostAttachment::getName,
                        InfoPostAttachment::getUrl,
                        InfoPostAttachment::getFileType,
                        InfoPostAttachment::getSortOrder)
                .containsExactly(
                        tuple("붙임1. 계획서.hwp", "https://inu/plan.hwp", "hwp", 0),
                        tuple("안내문.pdf", "https://inu/guide.pdf", "pdf", 1));
    }

    private Attachment attachment(String name, String url, String fileType) {
        Attachment attachment = mock(Attachment.class);
        lenient().when(attachment.getName()).thenReturn(name);
        lenient().when(attachment.getUrl()).thenReturn(url);
        lenient().when(attachment.getFileType()).thenReturn(fileType); // URL이 없으면 조회되지 않음
        return attachment;
    }
}
