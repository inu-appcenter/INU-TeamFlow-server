package com.inuteamflow.server.domain.user.service;

import com.inuteamflow.server.domain.chat.repository.ChatRoomMemberRepository;
import com.inuteamflow.server.domain.event.repository.*;
import com.inuteamflow.server.domain.fcm.repository.FcmTokenRepository;
import com.inuteamflow.server.domain.infoPost.repository.InfoPostImageRepository;
import com.inuteamflow.server.domain.infoPost.repository.InfoPostRepository;
import com.inuteamflow.server.domain.infoPost.repository.InfoPostScrapRepository;
import com.inuteamflow.server.domain.invitation.repository.TeamInvitationRepository;
import com.inuteamflow.server.domain.notification.repository.NotificationRepository;
import com.inuteamflow.server.domain.recruitment.repository.RecruitmentApplicationRepository;
import com.inuteamflow.server.domain.recruitment.repository.RecruitmentRepository;
import com.inuteamflow.server.domain.recruitment.repository.RecruitmentScrapRepository;
import com.inuteamflow.server.domain.team.enums.TeamRole;
import com.inuteamflow.server.domain.team.repository.TeamMemberRepository;
import com.inuteamflow.server.domain.teamNotice.repository.TeamNoticeImageRepository;
import com.inuteamflow.server.domain.teamNotice.repository.TeamNoticeReadRepository;
import com.inuteamflow.server.domain.teamNotice.repository.TeamNoticeRepository;
import com.inuteamflow.server.domain.user.dto.request.UserUpdateRequest;
import com.inuteamflow.server.domain.user.dto.response.MyInfoResponse;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.enums.UserConstants;
import com.inuteamflow.server.domain.user.repository.UserRepository;
import com.inuteamflow.server.domain.vote.repository.VoteAvailabilityRepository;
import com.inuteamflow.server.domain.vote.repository.VoteDateRepository;
import com.inuteamflow.server.domain.vote.repository.VoteParticipantRepository;
import com.inuteamflow.server.domain.vote.repository.VoteRepository;
import com.inuteamflow.server.domain.vote.repository.VoteResultRepository;
import com.inuteamflow.server.domain.vote.repository.VoteTimeSlotRepository;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import com.inuteamflow.server.global.jwt.refresh.RefreshTokenRepository;
import com.inuteamflow.server.global.s3.S3Service;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final S3Service s3Service;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final RecruitmentApplicationRepository recruitmentApplicationRepository;
    private final RecruitmentRepository recruitmentRepository;
    private final RecruitmentScrapRepository recruitmentScrapRepository;
    private final InfoPostImageRepository infoPostImageRepository;
    private final InfoPostRepository infoPostRepository;
    private final InfoPostScrapRepository infoPostScrapRepository;
    private final VoteAvailabilityRepository voteAvailabilityRepository;
    private final VoteParticipantRepository voteParticipantRepository;
    private final VoteResultRepository voteResultRepository;
    private final VoteTimeSlotRepository voteTimeSlotRepository;
    private final VoteDateRepository voteDateRepository;
    private final VoteRepository voteRepository;
    private final TeamInvitationRepository teamInvitationRepository;
    private final TeamNoticeReadRepository teamNoticeReadRepository;
    private final TeamNoticeImageRepository teamNoticeImageRepository;
    private final TeamNoticeRepository teamNoticeRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final RecurrenceExceptionParticipantRepository recurrenceExceptionParticipantRepository;
    private final RecurrenceExceptionRepository recurrenceExceptionRepository;
    private final RecurrenceRuleRepository recurrenceRuleRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final EventRepository eventRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final NotificationRepository notificationRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 로그인한 사용자의 정보를 조회한다.
     *
     * @param user 로그인한 사용자
     * @return 프로필 이미지 URL이 포함된 사용자 정보
     */
    public MyInfoResponse getMyInfo(User user) {
        String imageUrl = s3Service.getImageUrl(user.getImageKey());
        return MyInfoResponse.of(user, imageUrl);
    }

    /**
     * 로그인한 사용자의 정보를 수정한다.
     *
     * <p>프로필 이미지가 변경되면 기본 이미지를 제외한 기존 이미지를 삭제한다.</p>
     *
     * @param user 로그인한 사용자
     * @param request 수정할 사용자 정보
     * @return 수정된 사용자 정보
     * @throws RestApiException 사용자를 찾을 수 없는 경우
     */
    @Transactional
    public MyInfoResponse updateMyInfo(User user, UserUpdateRequest request) {
        // Repository 에서 로그인한 사용자를 조회 → 수정 사항을 DB에 저장하기 위함
        User requester = userRepository
                .findById(user.getUserId())
                .orElseThrow(() -> new RestApiException(CustomErrorCode.USER_NOT_FOUND));

        // 오래된 이미지키를 변경하기 위해 삭제 전 저장
        String oldImageKey = requester.getImageKey();

        // 사용자 정보 수정
        String encodedPassword =
                StringUtils.hasText(request.getPassword()) ? bCryptPasswordEncoder.encode(request.getPassword()) : null;
        requester.update(request, encodedPassword);

        // 요청 DTO에 담긴 이미지키가 새로운거라면 기존의 S3의 파일을 삭제
        String newImageKey = requester.getImageKey();
        if (StringUtils.hasText(oldImageKey)
                && !oldImageKey.equals(newImageKey)
                && !oldImageKey.equals(UserConstants.DEFAULT_PROFILE_KEY)) {
            s3Service.deleteImage(oldImageKey);
        }

        // 새로운 이미지키 저장
        String imageUrl = s3Service.getImageUrl(newImageKey);
        return MyInfoResponse.of(requester, imageUrl);
    }

    /**
     * 로그인한 사용자를 탈퇴 처리한다.
     *
     * <p>사용자와 연관된 데이터를 삭제하고 기본 이미지를 제외한 소유 이미지를 제거한다.</p>
     *
     * @param user 탈퇴할 사용자
     * @throws RestApiException 사용자가 팀장이거나 진행 중인 투표의 생성자인 경우
     */
    @Transactional
    public void deleteUser(User user) {
        // 이미지 키 수집
        // 정보글 이미지 → 팀 공지 이미지 → 사용자 프로필
        List<String> infoPostImages = infoPostImageRepository.findImageKeysByInfoPostCreatedBy(user.getUserId());
        List<String> teamNoticeImages = teamNoticeImageRepository.findImageKeysByTeamNoticeCreatedBy(user.getUserId());
        String userProfile = user.getImageKey();

        // 팀장 여부 체크
        if (teamMemberRepository.existsByUserAndTeamRole(user, TeamRole.LEADER)) {
            throw new RestApiException(CustomErrorCode.TEAM_MEMBER_IS_HOST);
        }
        // 진행 중인 투표 생성자 여부 체크
        if (voteRepository.existsByCreatedByAndIsOpenedTrue(user.getUserId())) {
            throw new RestApiException(CustomErrorCode.VOTE_IS_OPEN);
        }

        // RecruitmentScrap (본인이 스크랩한 것 + 본인 모집글을 스크랩한 다른 사람의 기록)
        recruitmentScrapRepository.deleteByUser(user);
        recruitmentScrapRepository.deleteByRecruitmentRecruiter(user);

        // RecruitmentApplication → RecruitmentApplication → Recruitment
        recruitmentApplicationRepository.deleteByCreatedBy(user.getUserId());
        recruitmentApplicationRepository.deleteByRecruitmentCreatedBy(user.getUserId());
        recruitmentRepository.deleteByRecruiter(user);

        // InfoPostScrap (본인이 스크랩한 것 + 본인 정보글을 스크랩한 다른 사람의 기록)
        infoPostScrapRepository.deleteByUser(user);
        infoPostScrapRepository.deleteByInfoPostCreatedBy(user.getUserId());

        // InfoPostImage → InfoPost
        recruitmentRepository.clearInfoPostByCreatedBy(user.getUserId());
        infoPostImageRepository.deleteByInfoPostCreatedBy(user.getUserId());
        infoPostRepository.deleteByCreatedBy(user.getUserId());

        // TeamInvitation
        teamInvitationRepository.deleteByReceiver(user);
        teamInvitationRepository.deleteByCreatedBy(user.getUserId());

        // 사용자가 생성한 종료 투표:
        // VoteAvailability → VoteParticipant/VoteResult → VoteTimeSlot → VoteDate → Vote
        voteAvailabilityRepository.deleteByClosedVoteCreatedBy(user.getUserId());
        voteParticipantRepository.deleteByClosedVoteCreatedBy(user.getUserId());
        voteResultRepository.deleteByClosedVoteCreatedBy(user.getUserId());
        voteTimeSlotRepository.deleteByClosedVoteCreatedBy(user.getUserId());
        voteDateRepository.deleteByClosedVoteCreatedBy(user.getUserId());
        voteRepository.deleteClosedByCreatedBy(user.getUserId());

        // 다른 사용자가 만든 투표에 참여한 데이터: VoteAvailability → VoteParticipant
        voteAvailabilityRepository.deleteByVoteParticipantTeamMemberUser(user);
        voteParticipantRepository.deleteByTeamMemberUser(user);

        // EventParticipant
        recurrenceExceptionParticipantRepository.deleteByTeamMemberUser(user);
        eventParticipantRepository.deleteByTeamMemberUser(user);

        // TeamNoticeRead (다른 사람의 공지를 읽은 경우) → TeamNoticeRead (사용자가 작성한 공지를 다른 사람이 읽은 경우) →
        // TeamNoticeImage → TeamNotice → TeamMember
        teamNoticeReadRepository.deleteByCreatedBy(user.getUserId());
        teamNoticeReadRepository.deleteByTeamNoticeCreatedBy(user.getUserId());
        teamNoticeImageRepository.deleteByTeamNoticeCreatedBy(user.getUserId());
        teamNoticeRepository.deleteByCreatedBy(user.getUserId());
        teamMemberRepository.deleteByUser(user);

        // ChatRoomMember (팀/1:1 채팅방 멤버십 전체 제거)
        chatRoomMemberRepository.deleteByUser(user);

        // 개인 일정: RecurrenceException → RecurrenceRule → Event
        recurrenceExceptionRepository.deleteByEventCreatedByAndTeamIsNull(user.getUserId());
        recurrenceRuleRepository.deleteByDaysByEventCreatedByAndTeamIsNull(user.getUserId());
        recurrenceRuleRepository.deleteByEventCreatedByAndTeamIsNull(user.getUserId());
        eventRepository.deleteByCreatedByAndTeamIsNull(user.getUserId());

        // FCM + Notification
        fcmTokenRepository.deleteByCreatedBy(user.getUserId());
        notificationRepository.deleteByReceiver(user);

        // RefreshToken
        refreshTokenRepository.deleteByUserId(user.getUserId());
        // User
        userRepository.deleteById(user.getUserId());

        // 정보글 이미지 삭제 → 팀 공지 이미지 삭제 → 사용자 프로필 삭제
        for (String infoPostImage : infoPostImages) {
            s3Service.deleteImage(infoPostImage);
        }
        for (String teamNoticeImage : teamNoticeImages) {
            s3Service.deleteImage(teamNoticeImage);
        }
        if (StringUtils.hasText(userProfile) && !userProfile.equals(UserConstants.DEFAULT_PROFILE_KEY)) {
            s3Service.deleteImage(userProfile);
        }
    }
}
