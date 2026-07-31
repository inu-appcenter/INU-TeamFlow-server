package com.inuteamflow.server.domain.recruitment.service;

import com.inuteamflow.server.domain.infoPost.entity.InfoPost;
import com.inuteamflow.server.domain.infoPost.repository.InfoPostRepository;
import com.inuteamflow.server.domain.recruitment.dto.request.RecruitmentCreateRequest;
import com.inuteamflow.server.domain.recruitment.dto.request.RecruitmentUpdateRequest;
import com.inuteamflow.server.domain.recruitment.dto.response.RecruitmentDetailResponse;
import com.inuteamflow.server.domain.recruitment.dto.response.RecruitmentSummaryResponse;
import com.inuteamflow.server.domain.recruitment.entity.Recruitment;
import com.inuteamflow.server.domain.recruitment.repository.RecruitmentApplicationRepository;
import com.inuteamflow.server.domain.recruitment.repository.RecruitmentRepository;
import com.inuteamflow.server.domain.team.entity.Team;
import com.inuteamflow.server.domain.team.repository.TeamRepository;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecruitmentService {

    private final RecruitmentRepository recruitmentRepository;
    private final RecruitmentApplicationRepository recruitmentApplicationRepository;
    private final TeamRepository teamRepository;
    private final InfoPostRepository infoPostRepository;

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 전체 모집글 목록을 조회한다.
     *
     * @param pageable 페이지 정보
     * @return 모집글 요약 목록
     */
    public Page<RecruitmentSummaryResponse> getRecruitments(Pageable pageable) {
        return recruitmentRepository.findAll(pageable).map(RecruitmentSummaryResponse::from);
    }

    /**
     * 사용자가 작성한 모집글 목록을 조회한다.
     *
     * @param user 모집자 사용자
     * @param pageable 페이지 정보
     * @return 사용자가 작성한 모집글 요약 목록
     */
    public Page<RecruitmentSummaryResponse> getMyRecruitments(User user, Pageable pageable) {
        return recruitmentRepository.findAllByRecruiter(user, pageable).map(RecruitmentSummaryResponse::from);
    }

    /**
     * 모집글 상세 정보를 조회한다.
     *
     * @param recruitmentId 조회할 모집글 ID
     * @param user 조회를 요청한 사용자
     * @return 모집자 여부와 신청 여부를 포함한 모집글 상세 정보
     * @throws RestApiException 모집글을 찾을 수 없는 경우
     */
    public RecruitmentDetailResponse getRecruitment(Long recruitmentId, User user) {
        Recruitment recruitment = recruitmentRepository
                .findById(recruitmentId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.RECRUITMENT_NOT_FOUND));

        boolean isRecruiter = recruitment.getRecruiter().getUserId().equals(user.getUserId());
        boolean hasApplied =
                recruitmentApplicationRepository.existsByRecruitmentAndCreatedBy(recruitment, user.getUserId());

        return RecruitmentDetailResponse.of(recruitment, isRecruiter, hasApplied);
    }

    /**
     * 모집글을 작성한다.
     *
     * <p>{@code infoPostId}가 지정되면 해당 정보글을 연결하고, 없으면 정보글 없이 작성한다.</p>
     *
     * @param request 작성할 모집글 정보
     * @param user 모집자 사용자
     * @return 작성된 모집글 상세 정보
     * @throws RestApiException 팀을 찾을 수 없거나 연결할 정보글을 찾을 수 없는 경우
     */
    @Transactional
    public RecruitmentDetailResponse createRecruitment(RecruitmentCreateRequest request, User user) {
        Team team = teamRepository
                .findById(request.getTeamId())
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_NOT_FOUND));

        InfoPost infoPost = null;
        if (request.getInfoPostId() != null) {
            infoPost = infoPostRepository
                    .findById(request.getInfoPostId())
                    .orElseThrow(() -> new RestApiException(CustomErrorCode.INFO_POST_NOT_FOUND));
        }

        Recruitment recruitment = Recruitment.builder()
                .title(request.getTitle())
                .category(request.getCategory())
                .description(request.getDescription())
                .targetMemberCount(request.getTargetMemberCount())
                .endAt(request.getEndAt())
                .team(team)
                .recruiter(user)
                .infoPost(infoPost)
                .build();

        recruitmentRepository.save(recruitment);

        return RecruitmentDetailResponse.of(recruitment, true, false);
    }

    /**
     * 모집글을 수정한다.
     *
     * <p>모집자 본인만 수정할 수 있다.</p>
     *
     * @param recruitmentId 수정할 모집글 ID
     * @param request 수정할 모집글 정보
     * @param user 수정을 요청한 사용자
     * @return 수정된 모집글 상세 정보
     * @throws RestApiException 모집글을 찾을 수 없거나 사용자가 모집자가 아닌 경우
     */
    @Transactional
    public RecruitmentDetailResponse updateRecruitment(
            Long recruitmentId, RecruitmentUpdateRequest request, User user) {
        Recruitment recruitment = recruitmentRepository
                .findById(recruitmentId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.RECRUITMENT_NOT_FOUND));

        if (!recruitment.getRecruiter().getUserId().equals(user.getUserId())) {
            throw new RestApiException(CustomErrorCode.RECRUITMENT_FORBIDDEN);
        }

        recruitment.update(
                request.getTitle(), request.getDescription(), request.getTargetMemberCount(), request.getEndAt());

        return RecruitmentDetailResponse.of(recruitment, true, false);
    }

    /**
     * 모집글을 삭제한다.
     *
     * <p>모집자 본인만 삭제할 수 있으며, 연결된 신청서를 먼저 삭제하여 FK 제약을 해소한다.</p>
     *
     * @param recruitmentId 삭제할 모집글 ID
     * @param user 삭제를 요청한 사용자
     * @throws RestApiException 모집글을 찾을 수 없거나 사용자가 모집자가 아닌 경우
     */
    @Transactional
    public void deleteRecruitment(Long recruitmentId, User user) {
        Recruitment recruitment = recruitmentRepository
                .findById(recruitmentId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.RECRUITMENT_NOT_FOUND));

        if (!recruitment.getRecruiter().getUserId().equals(user.getUserId())) {
            throw new RestApiException(CustomErrorCode.RECRUITMENT_FORBIDDEN);
        }

        recruitmentApplicationRepository.deleteAllByRecruitmentIn(List.of(recruitment));
        recruitmentRepository.delete(recruitment);
    }
}
