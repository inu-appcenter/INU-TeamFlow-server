package com.inuteamflow.server.domain.recruitment.service;

import com.inuteamflow.server.domain.recruitment.dto.request.ApplicationCreateRequest;
import com.inuteamflow.server.domain.recruitment.dto.request.ApplicationStatusUpdateRequest;
import com.inuteamflow.server.domain.recruitment.dto.response.ApplicationDetailResponse;
import com.inuteamflow.server.domain.recruitment.dto.response.ApplicationStatusResponse;
import com.inuteamflow.server.domain.recruitment.dto.response.ApplicationSummaryResponse;
import com.inuteamflow.server.domain.recruitment.dto.response.MyApplicationSummaryResponse;
import com.inuteamflow.server.domain.recruitment.entity.Recruitment;
import com.inuteamflow.server.domain.recruitment.entity.RecruitmentApplication;
import com.inuteamflow.server.domain.recruitment.repository.RecruitmentApplicationRepository;
import com.inuteamflow.server.domain.recruitment.repository.RecruitmentRepository;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.repository.UserRepository;
import com.inuteamflow.server.global.enums.Status;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecruitmentApplicationService {

    private final RecruitmentApplicationRepository recruitmentApplicationRepository;
    private final UserRepository userRepository;
    private final RecruitmentRepository recruitmentRepository;

    // 모집글에 신청하기
    @Transactional
    public ApplicationSummaryResponse apply (Long recruitmentId, ApplicationCreateRequest request, User user) {
        Recruitment recruitment = recruitmentRepository.findById(recruitmentId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.RECRUITMENT_NOT_FOUND));

        boolean isRecruiter = recruitment.getRecruiter().getUserId().equals(user.getUserId());
        boolean hasApplied = recruitmentApplicationRepository.existsByRecruitmentAndCreatedBy(recruitment, user.getUserId());

        if (!recruitment.getIsOpened()) {
            throw new RestApiException(CustomErrorCode.RECRUITMENT_CLOSED);
        }
        if (recruitment.getEndAt().isBefore(LocalDateTime.now())) {
            throw new RestApiException(CustomErrorCode.RECRUITMENT_EXPIRED);
        }

        if (isRecruiter) throw new RestApiException(CustomErrorCode.RECRUITMENT_APPLICANT_FORBIDDEN);
        if (hasApplied) throw new RestApiException(CustomErrorCode.RECRUITMENT_ALREADY_APPLIED);

        RecruitmentApplication application = RecruitmentApplication.builder()
                .recruitment(recruitment)
                .introduction(request.getIntroduction())
                .build();

        recruitmentApplicationRepository.save(application);

        return ApplicationSummaryResponse.of(application, user.getName());
    }

    // 모집글에 올라온 신청서 목록 조회
    public Page<ApplicationSummaryResponse> getApplicationsByRecruitment(Long recruitmentId, User user, Pageable pageable) {
        Recruitment recruitment = recruitmentRepository.findById(recruitmentId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.RECRUITMENT_NOT_FOUND));

        if (!recruitment.getRecruiter().getUserId().equals(user.getUserId())) {
            throw new RestApiException(CustomErrorCode.RECRUITMENT_FORBIDDEN);
        }

        return recruitmentApplicationRepository.findAllByRecruitment(recruitment, pageable)
                .map(application -> {
                    String applicantName = userRepository.findById(application.getCreatedBy())
                            .map(User::getName)
                            .orElse(null);
                    return ApplicationSummaryResponse.of(application, applicantName);
                });
    }

    // 내가 신청한 신청서 목록 조회
    public Page<MyApplicationSummaryResponse> getMyApplications(User user, Pageable pageable) {
        return recruitmentApplicationRepository.findAllByCreatedBy(user.getUserId(), pageable)
                .map(MyApplicationSummaryResponse::from);
    }

    // 신청서 상세 조회
    public ApplicationDetailResponse getApplication(Long applicationId, User user) {
        RecruitmentApplication recruitmentApplication = recruitmentApplicationRepository
                .findById(applicationId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.RECRUITMENT_APPLICATION_NOT_FOUND));

        boolean isRecruiter = recruitmentApplication.getRecruitment().getRecruiter().getUserId().equals(user.getUserId());

        if (!recruitmentApplication.getCreatedBy().equals(user.getUserId()) &&
            !isRecruiter) {
            throw new RestApiException(CustomErrorCode.RECRUITMENT_APPLICANT_FORBIDDEN);
        }

        String applicantName = userRepository.findById(recruitmentApplication.getCreatedBy())
                .map(User::getName)
                .orElse(null);

        return ApplicationDetailResponse.of(recruitmentApplication, applicantName, isRecruiter);
    }

    // 신청서 취소/수락/거절
    @Transactional
    public ApplicationStatusResponse updateStatus(Long applicationId, ApplicationStatusUpdateRequest request,
                                                  User user) {
        RecruitmentApplication recruitmentApplication = recruitmentApplicationRepository
                .findById(applicationId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.RECRUITMENT_APPLICATION_NOT_FOUND));

        Status newStatus = request.getApplicationStatus();

        if (newStatus == Status.CANCELED) {
            if (!recruitmentApplication.getCreatedBy().equals(user.getUserId())) {
                throw new RestApiException(CustomErrorCode.RECRUITMENT_APPLICANT_FORBIDDEN);
            }
            recruitmentApplication.cancel();

        } else if (newStatus == Status.ACCEPTED || newStatus == Status.DECLINED) {
            Recruitment recruitment = recruitmentApplication.getRecruitment();

            if (!recruitmentApplication.getRecruitment().getRecruiter().getUserId().equals(user.getUserId())) {
                throw new RestApiException(CustomErrorCode.RECRUITMENT_FORBIDDEN);
            }

            if (newStatus == Status.ACCEPTED) {
                recruitment.increaseCurrentMemberCount();
                recruitmentApplication.accept();
            }
            else recruitmentApplication.decline();
        }
        return ApplicationStatusResponse.from(recruitmentApplication);
    }
}
