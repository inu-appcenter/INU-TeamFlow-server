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

    /**
     * 프로필 이미지 업로드용 presigned URL을 발급한다.
     *
     * <p>클라이언트는 발급된 URL로 10분 안에 요청한 Content-Type 그대로 PUT 업로드해야 한다.</p>
     *
     * @param request 업로드할 파일명과 Content-Type
     * @return 업로드 URL과 업로드될 이미지의 S3 키
     * @throws RestApiException 허용되지 않은 이미지 Content-Type인 경우
     */
    public PresignedUrlResponse getProfilePresignedUrl(PresignedUrlRequest request) {
        validateImageContentType(request.getContentType());

        String imageKey = createProfileImageKey(request.getFileName());
        String uploadUrl = createUploadPresignedUrl(imageKey, request.getContentType());

        return PresignedUrlResponse.of(uploadUrl, imageKey);
    }

    /**
     * 팀 배너 이미지 업로드용 presigned URL을 발급한다.
     *
     * <p>클라이언트는 발급된 URL로 10분 안에 요청한 Content-Type 그대로 PUT 업로드해야 한다.</p>
     *
     * @param request 업로드할 파일명과 Content-Type
     * @return 업로드 URL과 업로드될 이미지의 S3 키
     * @throws RestApiException 허용되지 않은 이미지 Content-Type인 경우
     */
    public PresignedUrlResponse getTeamBannerPresignedUrl(PresignedUrlRequest request) {
        validateImageContentType(request.getContentType());

        String imageKey = createTeamBannerImageKey(request.getFileName());
        String uploadUrl = createUploadPresignedUrl(imageKey, request.getContentType());

        return PresignedUrlResponse.of(uploadUrl, imageKey);
    }

    /**
     * 팀 공지 이미지 여러 장의 업로드용 presigned URL을 발급한다.
     *
     * <p>요청 중 하나라도 허용되지 않은 Content-Type이면 전체 요청이 실패한다.</p>
     *
     * @param requests 업로드할 파일명과 Content-Type 목록
     * @return 요청 순서대로의 업로드 URL과 이미지 S3 키 목록
     * @throws RestApiException 허용되지 않은 이미지 Content-Type이 포함된 경우
     */
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

    /**
     * 정보글 이미지 여러 장의 업로드용 presigned URL을 발급한다.
     *
     * <p>요청 중 하나라도 허용되지 않은 Content-Type이면 전체 요청이 실패한다.</p>
     *
     * @param requests 업로드할 파일명과 Content-Type 목록
     * @return 요청 순서대로의 업로드 URL과 이미지 S3 키 목록
     * @throws RestApiException 허용되지 않은 이미지 Content-Type이 포함된 경우
     */
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

    /**
     * 채팅 이미지 업로드용 presigned URL을 발급한다.
     *
     * <p>클라이언트는 발급된 URL로 10분 안에 요청한 Content-Type 그대로 PUT 업로드해야 한다.</p>
     *
     * @param request 업로드할 파일명과 Content-Type
     * @return 업로드 URL과 업로드될 이미지의 S3 키
     * @throws RestApiException 허용되지 않은 이미지 Content-Type인 경우
     */
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

    /**
     * S3 이미지 키를 CloudFront 조회 URL로 변환한다.
     *
     * <p>설정된 CloudFront 도메인에 스킴이 없으면 {@code https://}를 붙인다.</p>
     *
     * @param imageKey 변환할 이미지 S3 키
     * @return CloudFront 이미지 URL, 키가 비어 있으면 {@code null}
     */
    public String getImageUrl(String imageKey) {
        if (!StringUtils.hasText(imageKey)) {
            return null;
        }

        String normalizedDomain =
                cloudFrontDomain.startsWith("http") ? cloudFrontDomain : "https://" + cloudFrontDomain;
        return normalizedDomain + "/" + imageKey;
    }

    /**
     * 팀 배너 이미지의 CloudFront URL을 반환한다.
     *
     * <p>배너 이미지가 없으면 팀 카테고리별 기본 배너({@code teams/banner/default/{카테고리}.png}) URL을 반환한다.</p>
     *
     * @param imageKey 팀 배너 이미지 S3 키, 없으면 {@code null}
     * @param category 기본 배너를 고를 팀 카테고리
     * @return 팀 배너 이미지 URL
     */
    public String getTeamImageUrl(String imageKey, Category category) {
        if (StringUtils.hasText(imageKey)) {
            return getImageUrl(imageKey);
        }
        String defaultKey = "teams/banner/default/" + category.name().toLowerCase(Locale.ROOT) + ".png";
        return getImageUrl(defaultKey);
    }

    /**
     * S3에서 이미지를 삭제한다.
     *
     * @param imageKey 삭제할 이미지 S3 키, 비어 있으면 아무것도 하지 않음
     */
    public void deleteImage(String imageKey) {
        if (!StringUtils.hasText(imageKey)) {
            return;
        }

        DeleteObjectRequest deleteObjectRequest =
                DeleteObjectRequest.builder().bucket(bucket).key(imageKey).build();

        s3Client.deleteObject(deleteObjectRequest);
    }

    /**
     * 업로드할 수 있는 이미지 Content-Type인지 검증한다.
     *
     * <p>jpeg, png, webp, gif만 허용하며 대소문자는 구분하지 않는다.</p>
     *
     * @param contentType 검증할 Content-Type
     * @throws RestApiException 허용되지 않은 Content-Type인 경우
     */
    private void validateImageContentType(String contentType) {
        if (!ALLOWED_IMAGE_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new RestApiException(CustomErrorCode.COMMON_INVALID_REQUEST);
        }
    }

    /**
     * 지정한 키와 Content-Type으로 업로드할 수 있는 PUT presigned URL을 만든다.
     *
     * <p>Content-Type이 서명에 포함되므로 업로드 요청의 Content-Type이 다르면 S3가 거부한다.</p>
     *
     * @param imageKey 업로드될 이미지 S3 키
     * @param contentType 업로드할 이미지 Content-Type
     * @return 10분간 유효한 업로드 URL
     */
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

    /**
     * 프로필 이미지 S3 키를 만든다.
     *
     * @param fileName 확장자를 뽑아낼 원본 파일명
     * @return {@code users/profile/{UUID}{확장자}} 형식의 키
     */
    private String createProfileImageKey(String fileName) {
        return "users/profile/" + UUID.randomUUID() + getFileExtension(fileName);
    }

    /**
     * 팀 배너 이미지 S3 키를 만든다.
     *
     * @param fileName 확장자를 뽑아낼 원본 파일명
     * @return {@code teams/banner/{UUID}{확장자}} 형식의 키
     */
    private String createTeamBannerImageKey(String fileName) {
        return "teams/banner/" + UUID.randomUUID() + getFileExtension(fileName);
    }

    /**
     * 팀 공지 이미지 S3 키를 만든다.
     *
     * @param fileName 확장자를 뽑아낼 원본 파일명
     * @return {@code teams/notices/{UUID}{확장자}} 형식의 키
     */
    private String createTeamNoticeImageKey(String fileName) {
        return "teams/notices/" + UUID.randomUUID() + getFileExtension(fileName);
    }

    /**
     * 파일명에서 확장자를 소문자로 뽑아낸다.
     *
     * @param fileName 원본 파일명
     * @return 점을 포함한 소문자 확장자(예: {@code .jpg}), 확장자가 없거나 점으로 끝나면 빈 문자열
     */
    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(dotIndex).toLowerCase(Locale.ROOT);
    }

    /**
     * 정보글 이미지 S3 키를 만든다.
     *
     * @param fileName 확장자를 뽑아낼 원본 파일명
     * @return {@code info-posts/image/{UUID}{확장자}} 형식의 키
     */
    private String createInfoPostImageKey(String fileName) {
        return "info-posts/image/" + UUID.randomUUID() + getFileExtension(fileName);
    }

    /**
     * 채팅 이미지 S3 키를 만든다.
     *
     * @param fileName 확장자를 뽑아낼 원본 파일명
     * @return {@code chat/image/{UUID}{확장자}} 형식의 키
     */
    private String createChatImageKey(String fileName) {
        return "chat/image/" + UUID.randomUUID() + getFileExtension(fileName);
    }
}
