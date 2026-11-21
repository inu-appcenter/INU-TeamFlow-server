package com.inuteamflow.server.global.s3;

import com.inuteamflow.server.global.enums.Category;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class S3Service {

    private static final Duration PRESIGNED_URL_EXPIRE_TIME = Duration.ofMinutes(10);
    private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.cloudfront.domain}")
    private String cloudFrontDomain;

    public PresignedUrlResponse getProfilePresignedUrl(PresignedUrlRequest request) {
        validateImageContentType(request.getContentType());

        String imageKey = createProfileImageKey(request.getFileName());
        String uploadUrl = createUploadPresignedUrl(imageKey, request.getContentType());

        return PresignedUrlResponse.of(uploadUrl, imageKey);
    }

    public PresignedUrlResponse getTeamBannerPresignedUrl(PresignedUrlRequest request) {
        validateImageContentType(request.getContentType());

        String imageKey = createTeamBannerImageKey(request.getFileName());
        String uploadUrl = createUploadPresignedUrl(imageKey, request.getContentType());

        return PresignedUrlResponse.of(uploadUrl, imageKey);
    }

    public List<PresignedUrlResponse> getTeamNoticeImagePresignedUrls(List<PresignedUrlRequest> requests) {
        return requests.stream()
                .map(request -> {
                    validateImageContentType(request.getContentType());
                    String imageKey = createTeamNoticeImageKey(request.getFileName());
                    String uploadUrl = createUploadPresignedUrl(imageKey, request.getContentType());
                    return PresignedUrlResponse.of(uploadUrl, imageKey);
                })
                .toList();
    }

    public List<PresignedUrlResponse> getInfoPostImagePresignedUrls(List<PresignedUrlRequest> requests) {
        return requests.stream()
                .map(request -> {
                    validateImageContentType(request.getContentType());
                    String imageKey = createInfoPostImageKey(request.getFileName());
                    String uploadUrl = createUploadPresignedUrl(imageKey, request.getContentType());
                    return PresignedUrlResponse.of(uploadUrl, imageKey);
                })
                .toList();
    }

    public PresignedUrlResponse getChatImagePresignedUrl(PresignedUrlRequest request) {
        validateImageContentType(request.getContentType());

        String imageKey = createChatImageKey(request.getFileName());
        String uploadUrl = createUploadPresignedUrl(imageKey, request.getContentType());

        return PresignedUrlResponse.of(uploadUrl, imageKey);
    }

    /**
     * 서버가 가진 이미지 파일을 정보글 이미지 경로로 S3에 직접 업로드한다.
     *
     * @param fileName 확장자를 뽑아낼 원본 파일명
     * @param contentType 저장할 이미지 Content-Type
     * @param content 이미지 바이트
     * @return 업로드된 이미지의 S3 키
     * @throws RestApiException 허용되지 않은 이미지 Content-Type인 경우
     */
    public String uploadInfoPostImage(String fileName, String contentType, byte[] content) {
        validateImageContentType(contentType);

        String imageKey = createInfoPostImageKey(fileName);
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(imageKey)
                .contentType(contentType)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(content));
        return imageKey;
    }

    public String getImageUrl(String imageKey) {
        if (!StringUtils.hasText(imageKey)) {
            return null;
        }

        String normalizedDomain =
                cloudFrontDomain.startsWith("http") ? cloudFrontDomain : "https://" + cloudFrontDomain;
        return normalizedDomain + "/" + imageKey;
    }

    public String getTeamImageUrl(String imageKey, Category category) {
        if (StringUtils.hasText(imageKey)) {
            return getImageUrl(imageKey);
        }
        String defaultKey = "teams/banner/default/" + category.name().toLowerCase(Locale.ROOT) + ".png";
        return getImageUrl(defaultKey);
    }

    public void deleteImage(String imageKey) {
        if (!StringUtils.hasText(imageKey)) {
            return;
        }

        DeleteObjectRequest deleteObjectRequest =
                DeleteObjectRequest.builder().bucket(bucket).key(imageKey).build();

        s3Client.deleteObject(deleteObjectRequest);
    }

    private void validateImageContentType(String contentType) {
        if (!ALLOWED_IMAGE_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new RestApiException(CustomErrorCode.COMMON_INVALID_REQUEST);
        }
    }

    private String createUploadPresignedUrl(String imageKey, String contentType) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(imageKey)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(PRESIGNED_URL_EXPIRE_TIME)
                .putObjectRequest(putObjectRequest)
                .build();

        return s3Presigner.presignPutObject(presignRequest).url().toString();
    }

    private String createProfileImageKey(String fileName) {
        return "users/profile/" + UUID.randomUUID() + getFileExtension(fileName);
    }

    private String createTeamBannerImageKey(String fileName) {
        return "teams/banner/" + UUID.randomUUID() + getFileExtension(fileName);
    }

    private String createTeamNoticeImageKey(String fileName) {
        return "teams/notices/" + UUID.randomUUID() + getFileExtension(fileName);
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(dotIndex).toLowerCase(Locale.ROOT);
    }

    private String createInfoPostImageKey(String fileName) {
        return "info-posts/image/" + UUID.randomUUID() + getFileExtension(fileName);
    }

    private String createChatImageKey(String fileName) {
        return "chat/image/" + UUID.randomUUID() + getFileExtension(fileName);
    }
}
