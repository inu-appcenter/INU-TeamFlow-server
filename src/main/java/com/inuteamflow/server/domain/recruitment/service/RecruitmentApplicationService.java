package com.inuteamflow.server.domain.recruitment.service;

import com.inuteamflow.server.domain.chat.service.ChatRoomService;
import com.inuteamflow.server.domain.notification.enums.NotificationType;
import com.inuteamflow.server.domain.notification.service.NotificationService;
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
import com.inuteamflow.server.domain.team.entity.TeamMember;
import com.inuteamflow.server.domain.team.enums.TeamRole;
import com.inuteamflow.server.domain.team.repository.TeamMemberRepository;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecruitmentApplicationService {

    private final RecruitmentApplicationRepository recruitmentApplicationRepository;
    private final UserRepository userRepository;
    private final RecruitmentRepository recruitmentRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ChatRoomService chatRoomService;
    private final NotificationService notificationService;

    // 모집글에 신청하기
    @Transactional
    public ApplicationSummaryResponse apply (Long recruitmentId, ApplicationCreateRequest request, User user) {

        if (!Boolean.TRUE.equals(user.getIsSchoolVerified())) {
            throw new RestApiException(CustomErrorCode.USER_SCHOOL_VERIFICATION_REQUIRED);
        }

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

        notificationService.createNotification(
                recruitment.getRecruiter(),
                "[" + recruitment.getTitle() + "] 새 지원자가 있어요",
                user.getName() + "님이 지원서를 보냈어요",
                NotificationType.APPLICATION,
                "/recruitment/" + recruitment.getRecruitmentId() + "/apply/applications/" + application.getRecruitmentApplicationId()
        );

        return ApplicationSummaryResponse.of(application, user.getName());
    }

    // 모집글에 올라온 신청서 목록 조회
    public Page<ApplicationSummaryResponse> getApplicationsByRecruitment(Long recruitmentId, User user, Pageable pageable) {
        Recruitment recruitment = recruitmentRepository.findById(recruitmentId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.RECRUITMENT_NOT_FOUND));

        if (!recruitment.getRecruiter().getUserId().equals(user.getUserId())) {
            throw new RestApiException(CustomErrorCode.RECRUITMENT_FORBIDDEN);
        }

        Page<RecruitmentApplication> applications =
                recruitmentApplicationRepository.findAllByRecruitment(recruitment, pageable);

        // 페이지 내 신청자 ID를 한 번에 모아서 조회
        List<Long> applicantIds = applications.stream()
                .map(RecruitmentApplication::getCreatedBy)
                .distinct()
                .toList();

        Map<Long, String> nameMap = userRepository.findAllById(applicantIds)
                .stream()
                .collect(Collectors.toMap(User::getUserId, User::getName));

        return applications.map(application -> {
            String applicantName = nameMap.get(application.getCreatedBy());
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

        User applicant = userRepository.findById(recruitmentApplication.getCreatedBy())
                .orElse(null);

        return ApplicationDetailResponse.of(
                recruitmentApplication,
                applicant != null ? applicant.getName() : null,
                applicant != null ? applicant.getDepartment() : null,
                applicant != null ? applicant.getStudentNumber() : null,
                isRecruiter
        );
    }

    // 신청서 취소 (신청자 본인만 가능)
    @Transactional
    public ApplicationStatusResponse cancelApplication(Long applicationId, User user) {
        RecruitmentApplication recruitmentApplication = recruitmentApplicationRepository
                .findById(applicationId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.RECRUITMENT_APPLICATION_NOT_FOUND));

        if (!recruitmentApplication.getCreatedBy().equals(user.getUserId())) {
            throw new RestApiException(CustomErrorCode.RECRUITMENT_APPLICANT_FORBIDDEN);
        }

        recruitmentApplication.cancel();
        return ApplicationStatusResponse.from(recruitmentApplication);
    }

    // 신청서 수락/거절 (모집자만 가능)
    @Transactional
    public ApplicationStatusResponse updateDecisionStatus(Long applicationId,
                                                          ApplicationStatusUpdateRequest request,
                                                          User user) {
        RecruitmentApplication recruitmentApplication = recruitmentApplicationRepository
                .findById(applicationId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.RECRUITMENT_APPLICATION_NOT_FOUND));

        Status newStatus = request.getApplicationStatus();

        if (newStatus != Status.ACCEPTED && newStatus != Status.DECLINED) {
            throw new RestApiException(CustomErrorCode.RECRUITMENT_APPLICATION_STATUS_INVALID);
        }

        Recruitment recruitment = recruitmentApplication.getRecruitment();

        if (!recruitment.getRecruiter().getUserId().equals(user.getUserId())) {
            throw new RestApiException(CustomErrorCode.RECRUITMENT_FORBIDDEN);
        }

        User applicant = userRepository.findById(recruitmentApplication.getCreatedBy())
                .orElseThrow(() -> new RestApiException(CustomErrorCode.USER_NOT_FOUND));

        if (newStatus == Status.ACCEPTED) {
            recruitment.increaseCurrentMemberCount();
            recruitmentApplication.accept();

            boolean alreadyMember = teamMemberRepository
                    .findByTeamAndUser(recruitment.getTeam(), applicant)
                    .isPresent();

            if (!alreadyMember) {
                teamMemberRepository.save(
                        TeamMember.create(recruitment.getTeam(), applicant, TeamRole.MEMBER)
                );
            }
            chatRoomService.addTeamChatRoomMember(recruitment.getTeam(), applicant);

            notificationService.createNotification(
                    applicant,
                    "합격을 축하드려요!",
                    "'" + recruitment.getTitle() + "' 모집에 합격하셨어요",
                    NotificationType.APPLICATION,
                    "/recruitment/" + recruitment.getRecruitmentId() + "/apply/applications/" + recruitmentApplication.getRecruitmentApplicationId()
            );
        } else {
            recruitmentApplication.decline();

            notificationService.createNotification(
                    applicant,
                    "지원 결과를 알려드려요",
                    "'" + recruitment.getTitle() + "' 모집에 아쉽게도 선발되지 못했어요",
                    NotificationType.APPLICATION,
                    "/recruitment/" + recruitment.getRecruitmentId() + "/apply/applications/" + recruitmentApplication.getRecruitmentApplicationId()
            );
        }

        return ApplicationStatusResponse.from(recruitmentApplication);
    }
}
