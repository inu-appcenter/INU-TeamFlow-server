package com.inuteamflow.server.global.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.inuteamflow.server.global.exception.error.RestApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * {@link S3Service#uploadInfoPostImage}가 지정한 Content-Type과 정보글 이미지 경로로 S3에 업로드하는지 검증한다.
 * - 실제 S3를 사용하지 않는다.
 */
class S3ServiceUploadTest {

    private final S3Client s3Client = mock(S3Client.class);
    private S3Service s3Service;

    @BeforeEach
    void setUp() {
        s3Service = new S3Service(s3Client, mock(S3Presigner.class));
        ReflectionTestUtils.setField(s3Service, "bucket", "test-bucket");
    }

    @Test
    @DisplayName("정보글 이미지 경로에 원본 확장자와 지정한 Content-Type으로 업로드한다")
    void uploadsWithContentTypeAndInfoPostKey() {
        String imageKey = s3Service.uploadInfoPostImage("포스터.JPG", "image/jpeg", new byte[] {1, 2, 3});

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        assertThat(captor.getValue().bucket()).isEqualTo("test-bucket");
        assertThat(captor.getValue().key()).isEqualTo(imageKey);
        assertThat(captor.getValue().contentType()).isEqualTo("image/jpeg");
        assertThat(imageKey).startsWith("info-posts/image/").endsWith(".jpg");
    }

    @Test
    @DisplayName("이미지가 아닌 Content-Type이면 업로드하지 않는다")
    void rejectsNonImageContentType() {
        assertThatThrownBy(() -> s3Service.uploadInfoPostImage("안내문.pdf", "application/pdf", new byte[] {1}))
                .isInstanceOf(RestApiException.class);

        verifyNoInteractions(s3Client);
    }
}
