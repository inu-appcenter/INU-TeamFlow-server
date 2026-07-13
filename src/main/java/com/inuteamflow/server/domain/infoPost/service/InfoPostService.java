package com.inuteamflow.server.domain.infoPost.service;

import com.inuteamflow.server.domain.infoPost.dto.request.InfoPostCreateRequest;
import com.inuteamflow.server.domain.infoPost.dto.request.InfoPostUpdateRequest;
import com.inuteamflow.server.domain.infoPost.dto.response.InfoPostDetailResponse;
import com.inuteamflow.server.domain.infoPost.dto.response.InfoPostSummaryResponse;
import com.inuteamflow.server.domain.infoPost.entity.InfoPost;
import com.inuteamflow.server.domain.infoPost.entity.InfoPostImage;
import com.inuteamflow.server.domain.infoPost.enums.InfoPostCategory;
import com.inuteamflow.server.domain.infoPost.enums.InfoPostType;
import com.inuteamflow.server.domain.infoPost.repository.InfoPostImageRepository;
import com.inuteamflow.server.domain.infoPost.repository.InfoPostRepository;
import com.inuteamflow.server.domain.recruitment.dto.response.RecruitmentSummaryResponse;
import com.inuteamflow.server.domain.recruitment.repository.RecruitmentRepository;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.repository.UserRepository;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import com.inuteamflow.server.global.s3.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InfoPostService {

    private final InfoPostRepository infoPostRepository;
    private final InfoPostImageRepository infoPostImageRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final RecruitmentRepository recruitmentRepository;

    // 정보글 목록 조회
    public Page<InfoPostSummaryResponse> getInfoPosts(InfoPostCategory category, InfoPostType type, String keyword, Pageable pageable) {
        List<InfoPostCategory> categories = resolveCategories(category, type);
        Page<InfoPost> infoPostPage = infoPostRepository.search(categories, keyword, pageable);
        return toSummaryPage(infoPostPage);
    }

    // 내가 작성한 정보글 목록 조회
    public Page<InfoPostSummaryResponse> getMyInfoPosts(User user, Pageable pageable) {
        Page<InfoPost> infoPostPage = infoPostRepository.findAllByCreatedBy(user.getUserId(), pageable);
        return toSummaryPage(infoPostPage);
    }

    // 정보글 상세 조회
    public InfoPostDetailResponse getInfoPost(Long infoPostId, User user) {
        InfoPost infoPost = getInfoPostById(infoPostId);
        List<InfoPostImage> images = infoPostImageRepository.findByInfoPostOrderBySortOrderAsc(infoPost);
        User author = getUserById(infoPost.getCreatedBy());
        String authorProfileUrl = s3Service.getImageUrl(author.getImageKey());
        boolean isAuthor = infoPost.isAuthor(user.getUserId());
        Integer recruitmentCount = getRecruitmentCount(infoPost);

        return InfoPostDetailResponse.of(infoPost, author, authorProfileUrl, images, s3Service::getImageUrl, isAuthor, recruitmentCount);
    }

    // 정보글 작성
    @Transactional
    public InfoPostDetailResponse createInfoPost(InfoPostCreateRequest request, User user) {

        if (!Boolean.TRUE.equals(user.getIsSchoolVerified())) {
            throw new RestApiException(CustomErrorCode.USER_SCHOOL_VERIFICATION_REQUIRED);
        }

        InfoPost infoPost = InfoPost.create(request.getCategory(), request.getTitle(), request.getContent());
        infoPostRepository.save(infoPost);

        List<InfoPostImage> images = saveImages(infoPost, request.getImageKeys());
        String authorProfileUrl = s3Service.getImageUrl(user.getImageKey());

        return InfoPostDetailResponse.of(infoPost, user, authorProfileUrl, images, s3Service::getImageUrl, true, getRecruitmentCount(infoPost));
    }

    // 정보글 수정
    @Transactional
    public InfoPostDetailResponse updateInfoPost(Long infoPostId, InfoPostUpdateRequest request, User user) {
        InfoPost infoPost = getInfoPostById(infoPostId);
        validateAuthor(infoPost, user);

        // 기존 이미지를 DB에서 지우기 전에 S3 키를 모아둬야 이후 삭제 가능
        List<InfoPostImage> oldImages = infoPostImageRepository.findByInfoPostOrderBySortOrderAsc(infoPost);
        infoPost.update(request.getTitle(), request.getContent());
        infoPostImageRepository.deleteByInfoPost(infoPost);
        oldImages.forEach(img -> s3Service.deleteImage(img.getImageKey()));
        List<InfoPostImage> images = saveImages(infoPost, request.getImageKeys());

        String authorProfileUrl = s3Service.getImageUrl(user.getImageKey());
        Integer recruitmentCount = getRecruitmentCount(infoPost);

        return InfoPostDetailResponse.of(infoPost, user, authorProfileUrl, images, s3Service::getImageUrl, true, recruitmentCount);
    }

    // 정보글 삭제
    @Transactional
    public void deleteInfoPost(Long infoPostId, User user) {
        InfoPost infoPost = getInfoPostById(infoPostId);
        validateAuthor(infoPost, user);

        List<InfoPostImage> images = infoPostImageRepository.findByInfoPostOrderBySortOrderAsc(infoPost);
        infoPostImageRepository.deleteByInfoPost(infoPost);
        images.forEach(img -> s3Service.deleteImage(img.getImageKey()));
        infoPostRepository.delete(infoPost);
    }

    // 이 정보글을 참조하는 모집글 목록 조회
    public Page<RecruitmentSummaryResponse> getRecruitmentsByInfoPost(Long infoPostId, Pageable pageable) {
        InfoPost infoPost = getInfoPostById(infoPostId);
        return recruitmentRepository.findAllByInfoPost(infoPost, pageable)
                .map(RecruitmentSummaryResponse::from);
    }

    /**
     * ==== 헬퍼 함수 ====
     */
    private Page<InfoPostSummaryResponse> toSummaryPage(Page<InfoPost> infoPostPage) {
        List<InfoPost> infoPosts = infoPostPage.getContent();

        // N+1 방지: 대표 이미지(sortOrder=0)를 한 번에 조회
        Map<Long, String> thumbnailKeyByInfoPostId = infoPosts.isEmpty() ? Map.of()
                : infoPostImageRepository.findAllByInfoPostInAndSortOrder(infoPosts, 0).stream()
                .collect(Collectors.toMap(img -> img.getInfoPost().getInfoPostId(), InfoPostImage::getImageKey));

        return infoPostPage.map(infoPost -> {
            String thumbnailKey = thumbnailKeyByInfoPostId.get(infoPost.getInfoPostId());
            String thumbnailUrl = thumbnailKey != null ? s3Service.getImageUrl(thumbnailKey) : null;
            Integer recruitmentCount = getRecruitmentCount(infoPost);
            return InfoPostSummaryResponse.of(infoPost, thumbnailUrl, recruitmentCount);
        });
    }

    private List<InfoPostImage> saveImages(InfoPost infoPost, List<String> imageKeys) {
        if (imageKeys == null || imageKeys.isEmpty()) {
            return List.of();
        }

        List<InfoPostImage> images = new ArrayList<>();
        for (int i = 0; i < imageKeys.size(); i++) {
            images.add(InfoPostImage.create(imageKeys.get(i), i, infoPost)); // 대표 이미지 = sortOrder 0
        }
        return infoPostImageRepository.saveAll(images);
    }

    private Integer getRecruitmentCount(InfoPost infoPost) {
        if (!infoPost.isLinkable()) {
            return null; // 자유형은 null
        }
        return (int) recruitmentRepository.countByInfoPost(infoPost);
    }

    private void validateAuthor(InfoPost infoPost, User user) {
        if (!infoPost.isAuthor(user.getUserId())) {
            throw new RestApiException(CustomErrorCode.INFO_POST_FORBIDDEN);
        }
    }

    private InfoPost getInfoPostById(Long infoPostId) {
        return infoPostRepository.findById(infoPostId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.INFO_POST_NOT_FOUND));
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.USER_NOT_FOUND));
    }

    private List<InfoPostCategory> resolveCategories(InfoPostCategory category, InfoPostType type) {
        if (category != null) {
            return List.of(category);
        }
        if (type != null) {
            return Arrays.stream(InfoPostCategory.values())
                    .filter(c -> c.getType() == type)
                    .toList();
        }
        return null;
    }

}
