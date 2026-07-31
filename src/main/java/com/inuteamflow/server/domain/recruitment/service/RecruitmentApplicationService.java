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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 모집글에 지원한다.
     *
     * <p>학교 인증된 사용자만 지원할 수 있으며, 모집이 마감되었거나 모집 기한이 지났으면 지원할 수 없다.
     * 모집자 본인은 지원할 수 없고, 이미 지원한 모집글에는 중복 지원할 수 없다. 지원서 저장 후 모집자에게
     * 알림을 보낸다.</p>
     *
     * @param recruitmentId 지원할 모집글 ID
     * @param request 지원 정보(자기소개)
     * @param user 지원하는 사용자
     * @return 지원자 이름을 포함한 지원서 요약 정보
     * @throws RestApiException 사용자가 학교 인증되지 않았거나, 모집글을 찾을 수 없거나, 모집이 마감되었거나,
     *                       모집 기한이 지났거나, 사용자가 모집자 본인이거나, 이미 지원한 경우
     */
    @Transactional
    public ApplicationSummaryResponse apply(Long recruitmentId, ApplicationCreateRequest request, User user) {

        if (!Boolean.TRUE.equals(user.getIsSchoolVerified())) {
            throw new RestApiException(CustomErrorCode.USER_SCHOOL_VERIFICATION_REQUIRED);
        }

        Recruitment recruitment = recruitmentRepository
                .findById(recruitmentId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.RECRUITMENT_NOT_FOUND));

        boolean isRecruiter = recruitment.getRecruiter().getUserId().equals(user.getUserId());
        boolean hasApplied =
                recruitmentApplicationRepository.existsByRecruitmentAndCreatedBy(recruitment, user.getUserId());

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
                "/recruitment/" + recruitment.getRecruitmentId() + "/apply/applications/"
                        + application.getRecruitmentApplicationId());

        return ApplicationSummaryResponse.of(application, user.getName());
    }

    /**
     * 모집글에 올라온 지원서 목록을 조회한다.
     *
     * <p>모집자 본인만 조회할 수 있으며, 페이지 내 지원자 이름을 한 번에 조회하여 N+1 문제를 방지한다.</p>
     *
     * @param recruitmentId 지원서를 조회할 모집글 ID
     * @param user 조회를 요청한 사용자
     * @param pageable 페이지 정보
     * @return 지원자 이름을 포함한 지원서 요약 목록
     * @throws RestApiException 모집글을 찾을 수 없거나 사용자가 모집자가 아닌 경우
     */
    public Page<ApplicationSummaryResponse> getApplicationsByRecruitment(
            Long recruitmentId, User user, Pageable pageable) {
        Recruitment recruitment = recruitmentRepository
                .findById(recruitmentId)
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

        Map<Long, String> nameMap = userRepository.findAllById(applicantIds).stream()
                .collect(Collectors.toMap(User::getUserId, User::getName));

        return applications.map(application -> {
            String applicantName = nameMap.get(application.getCreatedBy());
            return ApplicationSummaryResponse.of(application, applicantName);
        });
    }

    /**
     * 사용자가 신청한 지원서 목록을 조회한다.
     *
     * @param user 조회할 지원자 사용자
     * @param pageable 페이지 정보
     * @return 사용자가 신청한 지원서 요약 목록
     */
    public Page<MyApplicationSummaryResponse> getMyApplications(User user, Pageable pageable) {
        return recruitmentApplicationRepository
                .findAllByCreatedBy(user.getUserId(), pageable)
                .map(MyApplicationSummaryResponse::from);
    }

    /**
     * 지원서 상세 정보를 조회한다.
     *
     * <p>지원자 본인 또는 모집자만 조회할 수 있으며, 지원자를 찾을 수 없는 경우 지원자 관련 정보는
     * {@code null}로 채워진다.</p>
     *
     * @param applicationId 조회할 지원서 ID
     * @param user 조회를 요청한 사용자
     * @return 지원자 정보와 모집자 여부를 포함한 지원서 상세 정보
     * @throws RestApiException 지원서를 찾을 수 없거나, 사용자가 지원자 본인도 모집자도 아닌 경우
     */
    public ApplicationDetailResponse getApplication(Long applicationId, User user) {
        RecruitmentApplication recruitmentApplication = recruitmentApplicationRepository
                .findById(applicationId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.RECRUITMENT_APPLICATION_NOT_FOUND));

        boolean isRecruiter = recruitmentApplication
                .getRecruitment()
                .getRecruiter()
                .getUserId()
                .equals(user.getUserId());

        if (!recruitmentApplication.getCreatedBy().equals(user.getUserId()) && !isRecruiter) {
            throw new RestApiException(CustomErrorCode.RECRUITMENT_APPLICANT_FORBIDDEN);
        }

        User applicant =
                userRepository.findById(recruitmentApplication.getCreatedBy()).orElse(null);

        return ApplicationDetailResponse.of(
                recruitmentApplication,
                recruitmentApplication.getCreatedBy(),
                applicant != null ? applicant.getName() : null,
                applicant != null ? applicant.getDepartment() : null,
                applicant != null ? applicant.getStudentNumber() : null,
                isRecruiter);
    }

    /**
     * 지원서를 취소한다.
     *
     * <p>지원자 본인만 취소할 수 있다.</p>
     *
     * @param applicationId 취소할 지원서 ID
     * @param user 취소를 요청한 사용자
     * @return 취소 처리된 지원서 상태 정보
     * @throws RestApiException 지원서를 찾을 수 없거나 사용자가 지원자 본인이 아닌 경우
     */
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

    /**
     * 지원서를 수락하거나 거절한다.
     *
     * <p>모집자 본인만 처리할 수 있다. 수락 시 모집글의 현재 인원 수를 늘리고, 아직 팀원이 아니면 팀 멤버로
     * 등록한 뒤 팀 채팅방에 추가한다. 처리 결과는 지원자에게 알림으로 보낸다.</p>
     *
     * @param applicationId 처리할 지원서 ID
     * @param request 적용할 지원 상태({@link Status#ACCEPTED} 또는 {@link Status#DECLINED})
     * @param user 처리를 요청한 사용자
     * @return 처리된 지원서 상태 정보
     * @throws RestApiException 지원서를 찾을 수 없거나, 요청 상태가 수락/거절이 아니거나, 사용자가 모집자가
     *                       아니거나, 지원자를 찾을 수 없는 경우
     */
    @Transactional
    public ApplicationStatusResponse updateDecisionStatus(
            Long applicationId, ApplicationStatusUpdateRequest request, User user) {
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

        User applicant = userRepository
                .findById(recruitmentApplication.getCreatedBy())
                .orElseThrow(() -> new RestApiException(CustomErrorCode.USER_NOT_FOUND));

        if (newStatus == Status.ACCEPTED) {
            recruitment.increaseCurrentMemberCount();
            recruitmentApplication.accept();

            boolean alreadyMember = teamMemberRepository
                    .findByTeamAndUser(recruitment.getTeam(), applicant)
                    .isPresent();

            if (!alreadyMember) {
                teamMemberRepository.save(TeamMember.create(recruitment.getTeam(), applicant, TeamRole.MEMBER));
            }
            chatRoomService.addTeamChatRoomMember(recruitment.getTeam(), applicant);

            notificationService.createNotification(
                    applicant,
                    "합격을 축하드려요!",
                    "'" + recruitment.getTitle() + "' 모집에 합격하셨어요",
                    NotificationType.APPLICATION,
                    "/recruitment/" + recruitment.getRecruitmentId() + "/apply/applications/"
                            + recruitmentApplication.getRecruitmentApplicationId());
        } else {
            recruitmentApplication.decline();

            notificationService.createNotification(
                    applicant,
                    "지원 결과를 알려드려요",
                    "'" + recruitment.getTitle() + "' 모집에 아쉽게도 선발되지 못했어요",
                    NotificationType.APPLICATION,
                    "/recruitment/" + recruitment.getRecruitmentId() + "/apply/applications/"
                            + recruitmentApplication.getRecruitmentApplicationId());
        }

        return ApplicationStatusResponse.from(recruitmentApplication);
    }
}
