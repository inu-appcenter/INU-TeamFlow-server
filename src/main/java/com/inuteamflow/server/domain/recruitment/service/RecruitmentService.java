package com.inuteamflow.server.domain.recruitment.service;

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

    // 모집글 전체 목록 조회
    public Page<RecruitmentSummaryResponse> getRecruitments(Pageable pageable) {
        return recruitmentRepository.findAll(pageable)
                .map(RecruitmentSummaryResponse::from);
    }

    // 내가 작성한 모집글 목록 조회
    public Page<RecruitmentSummaryResponse> getMyRecruitments(User user, Pageable pageable) {
        return recruitmentRepository.findAllByRecruiter(user, pageable)
                .map(RecruitmentSummaryResponse::from);
    }

    // 모집글 상세 조회
    public RecruitmentDetailResponse getRecruitment(Long recruitmentId, User user) {
        Recruitment recruitment = recruitmentRepository.findById(recruitmentId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.RECRUITMENT_NOT_FOUND));

        boolean isRecruiter = recruitment.getRecruiter().getUserId().equals(user.getUserId());
        boolean hasApplied = recruitmentApplicationRepository.existsByRecruitmentAndCreatedBy(recruitment, user.getUserId());

        return RecruitmentDetailResponse.of(recruitment, isRecruiter, hasApplied);
    }

    // 모집글 작성
    @Transactional
    public RecruitmentDetailResponse createRecruitment(RecruitmentCreateRequest request, User user) {
        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_NOT_FOUND));

        Recruitment recruitment = Recruitment.builder()
                .title(request.getTitle())
                .category(request.getCategory())
                .description(request.getDescription())
                .targetMemberCount(request.getTargetMemberCount())
                .endAt(request.getEndAt())
                .team(team)
                .recruiter(user)
                .build();

        recruitmentRepository.save(recruitment);

        return RecruitmentDetailResponse.of(recruitment, true, false);
    }

    // 모집글 수정
    @Transactional
    public RecruitmentDetailResponse updateRecruitment(Long recruitmentId, RecruitmentUpdateRequest request, User user) {
        Recruitment recruitment = recruitmentRepository.findById(recruitmentId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.RECRUITMENT_NOT_FOUND));

        if (!recruitment.getRecruiter().getUserId().equals(user.getUserId())) {
            throw new RestApiException(CustomErrorCode.RECRUITMENT_FORBIDDEN);
        }

        recruitment.update(
                request.getTitle(),
                request.getDescription(),
                request.getRecruitmentCategory(),
                request.getTargetMemberCount(),
                request.getEndAt()
        );

        return RecruitmentDetailResponse.of(recruitment, true, false);
    }

}
