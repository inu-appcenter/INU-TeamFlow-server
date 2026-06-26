package com.inuteamflow.server.global.s3;

import com.inuteamflow.server.domain.team.entity.Team;
import com.inuteamflow.server.domain.team.entity.TeamMember;
import com.inuteamflow.server.domain.team.enums.TeamRole;
import com.inuteamflow.server.domain.team.repository.TeamMemberRepository;
import com.inuteamflow.server.domain.team.repository.TeamRepository;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class S3Service {

    private static final Duration PRESIGNED_URL_EXPIRE_TIME = Duration.ofMinutes(10);
    private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.cloudfront.domain}")
    private String cloudFrontDomain;

    public PresignedUrlResponse getProfilePresignedUrl(
            PresignedUrlRequest request
    ) {
        validateImageContentType(request.getContentType());

        String imageKey = createProfileImageKey(request.getFileName());
        String uploadUrl = createUploadPresignedUrl(imageKey, request.getContentType());

        return PresignedUrlResponse.create(uploadUrl, imageKey);
    }

    public PresignedUrlResponse getTeamBannerPresignedUrl(PresignedUrlRequest request) {
        validateImageContentType(request.getContentType());

        String imageKey = createTeamBannerImageKey(request.getFileName());
        String uploadUrl = createUploadPresignedUrl(imageKey, request.getContentType());

        return PresignedUrlResponse.create(uploadUrl, imageKey);
    }

    public List<PresignedUrlResponse> getTeamNoticeImagePresignedUrls(List<PresignedUrlRequest> requests) {
        return requests.stream()
                .map(request -> {
                    validateImageContentType(request.getContentType());
                    String imageKey = createTeamNoticeImageKey(request.getFileName());
                    String uploadUrl = createUploadPresignedUrl(imageKey, request.getContentType());
                    return PresignedUrlResponse.create(uploadUrl, imageKey);
                })
                .toList();
    }

    public String getImageUrl(String imageKey) {
        if (!StringUtils.hasText(imageKey)) {
            return null;
        }

        String normalizedDomain = cloudFrontDomain.startsWith("http")
                ? cloudFrontDomain
                : "https://" + cloudFrontDomain;
        return normalizedDomain + "/" + imageKey;
    }

    public void deleteImage(String imageKey) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(imageKey)
                .build();

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

        return s3Presigner.presignPutObject(presignRequest)
                .url()
                .toString();
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
}
