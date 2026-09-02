package com.inuteamflow.server.domain.infoPost.service;

import com.inuteamflow.server.domain.infoPost.dto.request.InfoPostCreateRequest;
import com.inuteamflow.server.domain.infoPost.dto.request.InfoPostUpdateRequest;
import com.inuteamflow.server.domain.infoPost.dto.response.InfoPostDetailResponse;
import com.inuteamflow.server.domain.infoPost.dto.response.InfoPostSummaryResponse;
import com.inuteamflow.server.domain.infoPost.entity.InfoPost;
import com.inuteamflow.server.domain.infoPost.entity.InfoPostImage;
import com.inuteamflow.server.domain.infoPost.entity.InfoPostScrap;
import com.inuteamflow.server.domain.infoPost.enums.InfoPostCategory;
import com.inuteamflow.server.domain.infoPost.enums.InfoPostType;
import com.inuteamflow.server.domain.infoPost.repository.InfoPostImageRepository;
import com.inuteamflow.server.domain.infoPost.repository.InfoPostRepository;
import com.inuteamflow.server.domain.infoPost.repository.InfoPostScrapRepository;
import com.inuteamflow.server.domain.recruitment.dto.response.RecruitmentSummaryResponse;
import com.inuteamflow.server.domain.recruitment.repository.RecruitmentRepository;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.repository.UserRepository;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import com.inuteamflow.server.global.s3.S3Service;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InfoPostService {

    private final InfoPostRepository infoPostRepository;
    private final InfoPostImageRepository infoPostImageRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final RecruitmentRepository recruitmentRepository;
    private final InfoPostScrapRepository infoPostScrapRepository;

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 조건에 맞는 정보글 목록을 조회한다.
     *
     * <p>{@code category}가 지정되면 해당 카테고리만, {@code category} 없이 {@code type}만 지정되면
     * 해당 유형에 속한 카테고리 전체를 대상으로 검색하며, 둘 다 없으면 전체 카테고리를 대상으로 한다.</p>
     *
     * @param category 조회할 정보글 카테고리, 전체 카테고리를 대상으로 할 경우 {@code null}
     * @param type 조회할 정보글 유형, {@code category}가 지정되었거나 전체 유형을 대상으로 할 경우 {@code null}
     * @param keyword 제목/내용 검색 키워드
     * @param pageable 페이지 정보
     * @return 대표 이미지와 모집글 수를 포함한 정보글 목록
     */
    public Page<InfoPostSummaryResponse> getInfoPosts(
            InfoPostCategory category, InfoPostType type, String keyword, Pageable pageable) {
        List<InfoPostCategory> categories = resolveCategories(category, type);
        Page<InfoPost> infoPostPage = infoPostRepository.search(categories, keyword, pageable);
        return toSummaryPage(infoPostPage);
    }

    /**
     * 사용자가 작성한 정보글 목록을 조회한다.
     *
     * @param user 작성자 사용자
     * @param pageable 페이지 정보
     * @return 대표 이미지와 모집글 수를 포함한 정보글 목록
     */
    public Page<InfoPostSummaryResponse> getMyInfoPosts(User user, Pageable pageable) {
        Page<InfoPost> infoPostPage = infoPostRepository.findAllByCreatedBy(user.getUserId(), pageable);
        return toSummaryPage(infoPostPage);
    }

    /**
     * 정보글 상세 정보를 조회한다.
     *
     * @param infoPostId 조회할 정보글 ID
     * @param user 조회를 요청한 사용자
     * @return 작성자 정보, 이미지 목록, 작성자 여부, 연관 모집글 수를 포함한 정보글 상세 정보
     * @throws RestApiException 정보글을 찾을 수 없는 경우
     */
    public InfoPostDetailResponse getInfoPost(Long infoPostId, User user) {
        InfoPost infoPost = getInfoPostById(infoPostId);
        List<InfoPostImage> images = infoPostImageRepository.findByInfoPostOrderBySortOrderAsc(infoPost);
        User author = getUserById(infoPost.getCreatedBy());
        String authorProfileUrl = s3Service.getImageUrl(author.getImageKey());
        boolean isAuthor = infoPost.isAuthor(user.getUserId());
        boolean isScrap = infoPostScrapRepository.existsByInfoPostAndUser(infoPost, user);
        Integer recruitmentCount = getRecruitmentCount(infoPost);

        return InfoPostDetailResponse.of(
                infoPost,
                author,
                authorProfileUrl,
                images,
                s3Service::getImageUrl,
                isAuthor,
                isScrap,
                recruitmentCount);
    }

    /**
     * 정보글을 작성한다.
     *
     * <p>학교 인증된 사용자만 작성할 수 있다.</p>
     *
     * @param request 작성할 정보글 정보
     * @param user 작성자 사용자
     * @return 작성된 정보글 상세 정보
     * @throws RestApiException 사용자가 학교 인증되지 않은 경우
     */
    @Transactional
    public InfoPostDetailResponse createInfoPost(InfoPostCreateRequest request, User user) {

        if (!Boolean.TRUE.equals(user.getIsSchoolVerified())) {
            throw new RestApiException(CustomErrorCode.USER_SCHOOL_VERIFICATION_REQUIRED);
        }

        InfoPost infoPost = InfoPost.create(request.getCategory(), request.getTitle(), request.getContent());
        infoPostRepository.save(infoPost);

        List<InfoPostImage> images = saveImages(infoPost, request.getImageKeys());
        String authorProfileUrl = s3Service.getImageUrl(user.getImageKey());

        return InfoPostDetailResponse.of(
                infoPost,
                user,
                authorProfileUrl,
                images,
                s3Service::getImageUrl,
                true,
                false,
                getRecruitmentCount(infoPost));
    }

    /**
     * 정보글을 수정한다.
     *
     * <p>작성자만 수정할 수 있으며, 기존 이미지를 모두 지우고 요청된 이미지로 다시 저장한다.
     * 기존 이미지의 S3 파일은 DB에서 삭제된 이후 함께 제거한다.</p>
     *
     * @param infoPostId 수정할 정보글 ID
     * @param request 수정할 정보글 정보
     * @param user 수정을 요청한 사용자
     * @return 수정된 정보글 상세 정보
     * @throws RestApiException 정보글을 찾을 수 없거나 사용자가 작성자가 아닌 경우
     */
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
        boolean isScrap = infoPostScrapRepository.existsByInfoPostAndUser(infoPost, user);
        Integer recruitmentCount = getRecruitmentCount(infoPost);

        return InfoPostDetailResponse.of(
                infoPost, user, authorProfileUrl, images, s3Service::getImageUrl, true, isScrap, recruitmentCount);
    }

    /**
     * 정보글을 삭제한다.
     *
     * <p>작성자만 삭제할 수 있으며, 정보글에 속한 이미지도 DB와 S3에서 함께 삭제한다.</p>
     *
     * @param infoPostId 삭제할 정보글 ID
     * @param user 삭제를 요청한 사용자
     * @throws RestApiException 정보글을 찾을 수 없거나 사용자가 작성자가 아닌 경우
     */
    @Transactional
    public void deleteInfoPost(Long infoPostId, User user) {
        InfoPost infoPost = getInfoPostById(infoPostId);
        validateAuthor(infoPost, user);

        deleteWithRelations(infoPost);
    }

    /**
     * 신고 처리에 따라 정보글을 강제 삭제한다.
     *
     * <p>관리자 권한으로 수행되므로 작성자 검증을 하지 않는다. 연관 데이터 정리는 일반 삭제와 동일하다.</p>
     *
     * @param infoPostId 삭제할 정보글 ID
     * @throws RestApiException 정보글을 찾을 수 없는 경우
     */
    @Transactional
    public void deleteInfoPostByAdmin(Long infoPostId) {
        deleteWithRelations(getInfoPostById(infoPostId));
    }

    /**
     * 해당 정보글을 참조하는 모집글 전체 목록을 조회한다.
     *
     * @param infoPostId 조회 기준이 되는 정보글 ID
     * @return 해당 정보글을 참조하는 모집글 전체 목록 (최신순)
     * @throws RestApiException 정보글을 찾을 수 없는 경우
     */
    public List<RecruitmentSummaryResponse> getRecruitmentsByInfoPost(Long infoPostId) {
        InfoPost infoPost = getInfoPostById(infoPostId);
        return recruitmentRepository.findAllByInfoPostOrderByCreatedAtDesc(infoPost).stream()
                .map(RecruitmentSummaryResponse::from)
                .toList();
    }

    /**
     * 정보글을 스크랩한다.
     *
     * @param infoPostId 스크랩할 정보글 ID
     * @param user 스크랩을 요청한 사용자
     * @throws RestApiException 정보글을 찾을 수 없거나 이미 스크랩한 경우
     */
    @Transactional
    public void scrapInfoPost(Long infoPostId, User user) {
        InfoPost infoPost = getInfoPostById(infoPostId);

        if (infoPostScrapRepository.existsByInfoPostAndUser(infoPost, user)) {
            throw new RestApiException(CustomErrorCode.INFO_POST_ALREADY_SCRAPPED);
        }

        infoPostScrapRepository.save(InfoPostScrap.create(infoPost, user));
    }

    /**
     * 정보글 스크랩을 취소한다.
     *
     * @param infoPostId 스크랩 취소할 정보글 ID
     * @param user 취소를 요청한 사용자
     * @throws RestApiException 정보글을 찾을 수 없거나 스크랩하지 않은 경우
     */
    @Transactional
    public void unscrapInfoPost(Long infoPostId, User user) {
        InfoPost infoPost = getInfoPostById(infoPostId);

        int deleted = infoPostScrapRepository.deleteByInfoPostAndUser(infoPost, user);
        if (deleted == 0) {
            throw new RestApiException(CustomErrorCode.INFO_POST_SCRAP_NOT_FOUND);
        }
    }

    /**
     * 사용자가 스크랩한 정보글 목록을 조회한다.
     *
     * @param user 조회를 요청한 사용자
     * @param pageable 페이지 정보
     * @return 스크랩한 시각 최신순으로 정렬된, 대표 이미지와 모집글 수를 포함한 정보글 목록
     */
    public Slice<InfoPostSummaryResponse> getMyInfoPostScraps(User user, Pageable pageable) {
        Slice<InfoPost> infoPostSlice = infoPostScrapRepository.findInfoPostsByUser(user, pageable);
        List<InfoPost> infoPosts = infoPostSlice.getContent();

        Map<Long, String> thumbnailKeyByInfoPostId = infoPosts.isEmpty()
                ? Map.of()
                : infoPostImageRepository.findAllByInfoPostInAndSortOrder(infoPosts, 0).stream()
                        .collect(
                                Collectors.toMap(img -> img.getInfoPost().getInfoPostId(), InfoPostImage::getImageKey));

        return infoPostSlice.map(infoPost -> {
            String thumbnailKey = thumbnailKeyByInfoPostId.get(infoPost.getInfoPostId());
            String thumbnailUrl = thumbnailKey != null ? s3Service.getImageUrl(thumbnailKey) : null;
            Integer recruitmentCount = getRecruitmentCount(infoPost);
            return InfoPostSummaryResponse.of(infoPost, thumbnailUrl, recruitmentCount);
        });
    }

    // =========================================================================
    // ================================ 헬퍼 함수 ================================
    // =========================================================================

    /**
     * 정보글과 연관 데이터를 함께 삭제한다.
     *
     * <p>정보글에 속한 이미지를 DB와 S3에서 삭제하고, 스크랩 기록을 정리한 후 정보글을 삭제한다.</p>
     *
     * @param infoPost 삭제할 정보글
     */
    private void deleteWithRelations(InfoPost infoPost) {
        List<InfoPostImage> images = infoPostImageRepository.findByInfoPostOrderBySortOrderAsc(infoPost);
        infoPostImageRepository.deleteByInfoPost(infoPost);
        images.forEach(img -> s3Service.deleteImage(img.getImageKey()));
        infoPostScrapRepository.deleteByInfoPost(infoPost);
        infoPostRepository.delete(infoPost);
    }

    /**
     * 정보글 페이지를 대표 이미지와 모집글 수를 포함한 요약 응답 페이지로 변환한다.
     *
     * <p>정보글별 대표 이미지({@code sortOrder}가 0인 이미지)를 한 번에 조회하여 N+1 문제를 방지한다.</p>
     *
     * @param infoPostPage 변환할 정보글 페이지
     * @return 대표 이미지와 모집글 수를 포함한 정보글 요약 페이지
     */
    private Page<InfoPostSummaryResponse> toSummaryPage(Page<InfoPost> infoPostPage) {
        List<InfoPost> infoPosts = infoPostPage.getContent();

        // N+1 방지: 대표 이미지(sortOrder=0)를 한 번에 조회
        Map<Long, String> thumbnailKeyByInfoPostId = infoPosts.isEmpty()
                ? Map.of()
                : infoPostImageRepository.findAllByInfoPostInAndSortOrder(infoPosts, 0).stream()
                        .collect(
                                Collectors.toMap(img -> img.getInfoPost().getInfoPostId(), InfoPostImage::getImageKey));

        return infoPostPage.map(infoPost -> {
            String thumbnailKey = thumbnailKeyByInfoPostId.get(infoPost.getInfoPostId());
            String thumbnailUrl = thumbnailKey != null ? s3Service.getImageUrl(thumbnailKey) : null;
            Integer recruitmentCount = getRecruitmentCount(infoPost);
            return InfoPostSummaryResponse.of(infoPost, thumbnailUrl, recruitmentCount);
        });
    }

    /**
     * 이미지 키 목록으로 정보글 이미지를 생성해 저장한다.
     *
     * <p>{@code imageKeys}가 {@code null}이거나 비어 있으면 저장하지 않으며, 목록의 첫 번째 이미지가
     * {@code sortOrder} 0인 대표 이미지가 된다.</p>
     *
     * @param infoPost 이미지를 연결할 정보글
     * @param imageKeys 저장할 이미지 키 목록
     * @return 저장된 정보글 이미지 목록
     */
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

    /**
     * 정보글에 연결된 모집글 수를 조회한다.
     *
     * <p>모집글과 연결될 수 없는 자유형 정보글은 {@code null}을 반환한다.</p>
     *
     * @param infoPost 모집글 수를 조회할 정보글
     * @return 연결된 모집글 수, 자유형 정보글이면 {@code null}
     */
    private Integer getRecruitmentCount(InfoPost infoPost) {
        if (!infoPost.isLinkable()) {
            return null; // 자유형은 null
        }
        return (int) recruitmentRepository.countByInfoPost(infoPost);
    }

    /**
     * 사용자가 정보글의 작성자인지 검증한다.
     *
     * @param infoPost 검증할 정보글
     * @param user 검증할 사용자
     * @throws RestApiException 사용자가 정보글의 작성자가 아닌 경우
     */
    private void validateAuthor(InfoPost infoPost, User user) {
        if (!infoPost.isAuthor(user.getUserId())) {
            throw new RestApiException(CustomErrorCode.INFO_POST_FORBIDDEN);
        }
    }

    /**
     * ID로 정보글을 조회한다.
     *
     * @param infoPostId 조회할 정보글 ID
     * @return 조회된 정보글
     * @throws RestApiException 정보글을 찾을 수 없는 경우
     */
    private InfoPost getInfoPostById(Long infoPostId) {
        return infoPostRepository
                .findById(infoPostId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.INFO_POST_NOT_FOUND));
    }

    /**
     * ID로 사용자를 조회한다.
     *
     * @param userId 조회할 사용자 ID
     * @return 조회된 사용자
     * @throws RestApiException 사용자를 찾을 수 없는 경우
     */
    private User getUserById(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new RestApiException(CustomErrorCode.USER_NOT_FOUND));
    }

    /**
     * 카테고리 또는 유형 조건으로 검색 대상 카테고리 목록을 결정한다.
     *
     * <p>{@code category}가 지정되면 해당 카테고리 하나만, 없이 {@code type}만 지정되면 해당 유형에 속한
     * 카테고리 전체를, 둘 다 없으면 전체 카테고리 대상임을 뜻하는 {@code null}을 반환한다.</p>
     *
     * @param category 검색할 정보글 카테고리, 지정하지 않은 경우 {@code null}
     * @param type 검색할 정보글 유형, 지정하지 않은 경우 {@code null}
     * @return 검색 대상 카테고리 목록, 전체 카테고리를 대상으로 할 경우 {@code null}
     */
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
