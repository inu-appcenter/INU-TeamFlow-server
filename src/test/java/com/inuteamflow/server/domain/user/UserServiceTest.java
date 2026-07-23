package com.inuteamflow.server.domain.user;

import com.inuteamflow.server.domain.chat.entity.ChatRoom;
import com.inuteamflow.server.domain.chat.entity.ChatRoomMember;
import com.inuteamflow.server.domain.chat.repository.ChatRoomMemberRepository;
import com.inuteamflow.server.domain.chat.repository.ChatRoomRepository;
import com.inuteamflow.server.domain.event.entity.Event;
import com.inuteamflow.server.domain.event.entity.EventParticipant;
import com.inuteamflow.server.domain.event.entity.RecurrenceException;
import com.inuteamflow.server.domain.event.entity.RecurrenceExceptionParticipant;
import com.inuteamflow.server.domain.event.entity.RecurrenceRule;
import com.inuteamflow.server.domain.event.enums.EventColor;
import com.inuteamflow.server.domain.event.enums.EventRole;
import com.inuteamflow.server.domain.event.enums.RecurrenceExceptionType;
import com.inuteamflow.server.domain.event.enums.RecurrenceFrequency;
import com.inuteamflow.server.domain.event.repository.EventParticipantRepository;
import com.inuteamflow.server.domain.event.repository.EventRepository;
import com.inuteamflow.server.domain.event.repository.RecurrenceExceptionParticipantRepository;
import com.inuteamflow.server.domain.event.repository.RecurrenceExceptionRepository;
import com.inuteamflow.server.domain.event.repository.RecurrenceRuleRepository;
import com.inuteamflow.server.domain.fcm.entity.FcmToken;
import com.inuteamflow.server.domain.fcm.repository.FcmTokenRepository;
import com.inuteamflow.server.domain.infoPost.entity.InfoPost;
import com.inuteamflow.server.domain.infoPost.entity.InfoPostImage;
import com.inuteamflow.server.domain.infoPost.enums.InfoPostCategory;
import com.inuteamflow.server.domain.infoPost.repository.InfoPostImageRepository;
import com.inuteamflow.server.domain.infoPost.repository.InfoPostRepository;
import com.inuteamflow.server.domain.invitation.entity.TeamInvitation;
import com.inuteamflow.server.domain.invitation.repository.TeamInvitationRepository;
import com.inuteamflow.server.domain.notification.entity.Notification;
import com.inuteamflow.server.domain.notification.enums.NotificationType;
import com.inuteamflow.server.domain.notification.repository.NotificationRepository;
import com.inuteamflow.server.domain.recruitment.entity.Recruitment;
import com.inuteamflow.server.domain.recruitment.entity.RecruitmentApplication;
import com.inuteamflow.server.domain.recruitment.repository.RecruitmentApplicationRepository;
import com.inuteamflow.server.domain.recruitment.repository.RecruitmentRepository;
import com.inuteamflow.server.domain.team.entity.Team;
import com.inuteamflow.server.domain.team.entity.TeamMember;
import com.inuteamflow.server.domain.team.enums.TeamRole;
import com.inuteamflow.server.domain.team.repository.TeamMemberRepository;
import com.inuteamflow.server.domain.team.repository.TeamRepository;
import com.inuteamflow.server.domain.teamNotice.entity.TeamNotice;
import com.inuteamflow.server.domain.teamNotice.entity.TeamNoticeImage;
import com.inuteamflow.server.domain.teamNotice.entity.TeamNoticeRead;
import com.inuteamflow.server.domain.teamNotice.repository.TeamNoticeImageRepository;
import com.inuteamflow.server.domain.teamNotice.repository.TeamNoticeReadRepository;
import com.inuteamflow.server.domain.teamNotice.repository.TeamNoticeRepository;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import com.inuteamflow.server.domain.user.enums.Department;
import com.inuteamflow.server.domain.user.enums.Role;
import com.inuteamflow.server.domain.user.repository.UserRepository;
import com.inuteamflow.server.domain.user.service.UserService;
import com.inuteamflow.server.domain.vote.entity.Vote;
import com.inuteamflow.server.domain.vote.entity.VoteAvailability;
import com.inuteamflow.server.domain.vote.entity.VoteDate;
import com.inuteamflow.server.domain.vote.entity.VoteParticipant;
import com.inuteamflow.server.domain.vote.entity.VoteTimeSlot;
import com.inuteamflow.server.domain.vote.repository.VoteAvailabilityRepository;
import com.inuteamflow.server.domain.vote.repository.VoteDateRepository;
import com.inuteamflow.server.domain.vote.repository.VoteParticipantRepository;
import com.inuteamflow.server.domain.vote.repository.VoteRepository;
import com.inuteamflow.server.domain.vote.repository.VoteTimeSlotRepository;
import com.inuteamflow.server.global.enums.Category;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import com.inuteamflow.server.global.jwt.refresh.RefreshToken;
import com.inuteamflow.server.global.jwt.refresh.RefreshTokenRepository;
import com.inuteamflow.server.global.s3.S3Service;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link UserService}의 회원 탈퇴 기능을 실제 Repository와 H2 데이터베이스로 검증한다.
 * - 사용자와 연관된 도메인 데이터를 구성한 뒤 삭제 순서, FK 무결성, 연관 데이터 정리 및 탈퇴 제한 조건을 확인한다.
 * - 각 테스트는 테스트 트랜잭션 안에서 실행되며 종료 후 롤백된다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceTest {

    @Autowired private UserService userService;
    @Autowired private EntityManager entityManager;

    @MockitoBean private S3Service s3Service; // 실제 AWS 호출 방지

    @Autowired private UserRepository userRepository;
    @Autowired private TeamRepository teamRepository;
    @Autowired private TeamMemberRepository teamMemberRepository;
    @Autowired private RecruitmentRepository recruitmentRepository;
    @Autowired private RecruitmentApplicationRepository recruitmentApplicationRepository;
    @Autowired private InfoPostRepository infoPostRepository;
    @Autowired private InfoPostImageRepository infoPostImageRepository;
    @Autowired private TeamInvitationRepository teamInvitationRepository;
    @Autowired private VoteRepository voteRepository;
    @Autowired private VoteParticipantRepository voteParticipantRepository;
    @Autowired private VoteAvailabilityRepository voteAvailabilityRepository;
    @Autowired private VoteDateRepository voteDateRepository;
    @Autowired private VoteTimeSlotRepository voteTimeSlotRepository;
    @Autowired private TeamNoticeRepository teamNoticeRepository;
    @Autowired private TeamNoticeImageRepository teamNoticeImageRepository;
    @Autowired private TeamNoticeReadRepository teamNoticeReadRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private EventParticipantRepository eventParticipantRepository;
    @Autowired private RecurrenceRuleRepository recurrenceRuleRepository;
    @Autowired private RecurrenceExceptionRepository recurrenceExceptionRepository;
    @Autowired private RecurrenceExceptionParticipantRepository recurrenceExceptionParticipantRepository;
    @Autowired private FcmTokenRepository fcmTokenRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private ChatRoomRepository chatRoomRepository;
    @Autowired private ChatRoomMemberRepository chatRoomMemberRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("계정 삭제 시 연관된 모든 엔티티가 연관관계 문제 없이 정리된다")
    void deleteUser_cleansUpAllRelatedEntitiesWithoutRelationalErrors() {
        // ===== given: target 계정과 얽힌 모든 도메인 엔티티 세팅 =====
        User target = createUser("target1", "target1@inu.ac.kr");
        User leader = createUser("leader1", "leader1@inu.ac.kr");
        User other = createUser("other1", "other1@inu.ac.kr");

        actingAs(leader);
        Team team = teamRepository.save(
                Team.builder().name("team1").description("desc").category(Category.PROJECT).build());
        TeamMember leaderMember = teamMemberRepository.save(TeamMember.create(team, leader, TeamRole.LEADER));

        actingAs(target);
        TeamMember targetMember = teamMemberRepository.save(TeamMember.create(team, target, TeamRole.MEMBER));

        // Recruitment/RecruitmentApplication: target이 만든 모집글(R1) + 남이 만든 모집글에 target이 지원(A2)
        actingAs(target);
        InfoPost infoPost = infoPostRepository.save(
                InfoPost.create(InfoPostCategory.CLUB, "정보글", "내용"));
        InfoPostImage infoPostImage = infoPostImageRepository.save(
                InfoPostImage.create("info-posts/image/target.png", 0, infoPost));
        Recruitment r1 = recruitmentRepository.save(Recruitment.builder()
                .title("target 모집").description("설명").category(Category.PROJECT)
                .targetMemberCount(5).endAt(LocalDateTime.now().plusDays(7))
                .team(team).recruiter(target).build());

        actingAs(other);
        recruitmentApplicationRepository.save(RecruitmentApplication.builder()
                .recruitment(r1).introduction("other 지원").build());

        actingAs(leader);
        Recruitment r2 = recruitmentRepository.save(Recruitment.builder()
                .title("leader 모집").description("설명").category(Category.PROJECT)
                .targetMemberCount(5).endAt(LocalDateTime.now().plusDays(7))
                .team(team).recruiter(leader).build());
        // target의 InfoPost를 연결한, leader가 만든 모집글(R3) -> clearInfoPostByCreatedBy 검증용
        Recruitment r3 = recruitmentRepository.save(Recruitment.builder()
                .title("leader 모집2").description("설명").category(Category.PROJECT)
                .targetMemberCount(5).endAt(LocalDateTime.now().plusDays(7))
                .team(team).recruiter(leader).infoPost(infoPost).build());

        actingAs(target);
        recruitmentApplicationRepository.save(RecruitmentApplication.builder()
                .recruitment(r2).introduction("target 지원").build());

        // TeamInvitation: target이 받은 초대 + target이 보낸 초대
        actingAs(leader);
        teamInvitationRepository.save(TeamInvitation.create(team, target));
        actingAs(target);
        teamInvitationRepository.save(TeamInvitation.create(team, leader));

        // Vote/VoteParticipant/VoteAvailability: leader가 만든 투표에 target이 참여
        actingAs(leader);
        Vote vote = voteRepository.save(Vote.builder()
                .team(team).title("투표").description("설명").isOpened(true).isAllDay(false)
                .dailyTimeStart(LocalTime.of(9, 0)).dailyTimeEnd(LocalTime.of(20, 0))
                .slotUnitMinute(30).build());
        VoteDate voteDate = voteDateRepository.save(VoteDate.create(vote, LocalDate.now().plusDays(1)));
        VoteTimeSlot voteTimeSlot = voteTimeSlotRepository.save(
                VoteTimeSlot.create(voteDate, LocalTime.of(9, 0), LocalTime.of(9, 30)));
        VoteParticipant voteParticipant = voteParticipantRepository.save(
                VoteParticipant.create(vote, targetMember));
        voteAvailabilityRepository.save(VoteAvailability.create(voteParticipant, voteTimeSlot));

        // TeamNotice: target이 작성, leader가 읽음
        actingAs(target);
        TeamNotice teamNotice = teamNoticeRepository.save(TeamNotice.builder()
                .team(team).title("공지").content("내용").isPinned(false).build());
        teamNoticeImageRepository.save(
                TeamNoticeImage.create("teams/notices/target.png", 0, teamNotice));
        actingAs(leader);
        teamNoticeReadRepository.save(TeamNoticeRead.create(teamNotice));

        // Event: 팀 일정(E1, team 존재) - team이 있으므로 개인일정 삭제 대상이 아님, 참여자 행만 제거되어야 함
        actingAs(target);
        Event teamEvent = eventRepository.save(Event.builder()
                .team(team).title("팀 일정")
                .startAt(LocalDateTime.now()).endAt(LocalDateTime.now().plusHours(1))
                .isAllDay(false).color(EventColor.SUN).uid("uid-team-event")
                .sequence(0).isFinished(false).isSingle(true).build());
        eventParticipantRepository.save(EventParticipant.create(teamEvent, targetMember, EventRole.HOST));
        RecurrenceException teamEventException = recurrenceExceptionRepository.save(RecurrenceException.builder()
                .event(teamEvent).originalOccurrenceAt(LocalDateTime.now())
                .exceptionType(RecurrenceExceptionType.CANCELLED).build());
        recurrenceExceptionParticipantRepository.save(
                RecurrenceExceptionParticipant.create(teamEventException, targetMember, EventRole.HOST));

        // Event: 개인 일정(E2, team=null) - createdBy=target, team이 null이므로 통째로 삭제되어야 함
        Event personalEvent = eventRepository.save(Event.builder()
                .title("개인 일정")
                .startAt(LocalDateTime.now()).endAt(LocalDateTime.now().plusHours(1))
                .isAllDay(false).color(EventColor.LEAF).uid("uid-personal-event")
                .sequence(0).isFinished(false).isSingle(false).build());
        recurrenceRuleRepository.save(RecurrenceRule.builder()
                .event(personalEvent).freq(RecurrenceFrequency.WEEKLY).intervalValue(1)
                .byDay(List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))
                .seriesStartAt(LocalDateTime.now()).build());
        recurrenceExceptionRepository.save(RecurrenceException.builder()
                .event(personalEvent).originalOccurrenceAt(LocalDateTime.now().plusDays(7))
                .exceptionType(RecurrenceExceptionType.CANCELLED).build());

        // FcmToken
        fcmTokenRepository.save(FcmToken.builder().fcmToken("fcm-token-target").deviceType("WEB").build());

        // Notification: target이 받은 알림(삭제 대상) + target이 보낸 알림(receiver=leader, 삭제되면 안됨)
        notificationRepository.save(Notification.create(
                target, "제목", "내용", NotificationType.CHAT, "/url"));
        notificationRepository.save(Notification.create(
                leader, "제목2", "내용2", NotificationType.CHAT, "/url2"));

        // RefreshToken
        refreshTokenRepository.save(RefreshToken.builder().user(target).refreshToken("refresh-token").build());

        // ChatRoom/ChatRoomMember: 팀 채팅방 + 1:1 채팅방 양쪽에 target 소속
        ChatRoom teamChatRoom = chatRoomRepository.save(ChatRoom.createTeamRoom(team));
        chatRoomMemberRepository.save(ChatRoomMember.create(teamChatRoom, leader));
        chatRoomMemberRepository.save(ChatRoomMember.create(teamChatRoom, target));

        ChatRoom directChatRoom = chatRoomRepository.save(ChatRoom.createDirectRoom());
        chatRoomMemberRepository.save(ChatRoomMember.create(directChatRoom, target));
        chatRoomMemberRepository.save(ChatRoomMember.create(directChatRoom, other));

        entityManager.flush();
        entityManager.clear();

        Long targetId = target.getUserId();
        Long leaderId = leader.getUserId();
        Long teamId = team.getTeamId();
        Long r2Id = r2.getRecruitmentId();
        Long r3Id = r3.getRecruitmentId();
        Long teamEventId = teamEvent.getEventId();
        Long teamChatRoomId = teamChatRoom.getChatRoomId();
        Long directChatRoomId = directChatRoom.getChatRoomId();

        User targetRef = userRepository.getReferenceById(targetId);

        // ===== when: FK 위반 등 연관관계 문제 없이 예외 없이 완료되어야 한다 =====
        userService.deleteUser(targetRef);
        entityManager.flush();
        entityManager.clear();

        // ===== then =====
        assertThat(userRepository.findById(targetId)).isEmpty();
        assertThat(userRepository.findById(leaderId)).isPresent();

        assertThat(teamRepository.findById(teamId)).isPresent(); // 팀 자체는 유지
        assertThat(teamMemberRepository.count()).isEqualTo(1); // leader만 남음

        assertThat(recruitmentRepository.count()).isEqualTo(2); // r1(target 소유) 삭제, r2/r3만 남음
        assertThat(recruitmentRepository.findById(r2Id)).isPresent();
        assertThat(recruitmentRepository.findById(r3Id)).isPresent();
        assertThat(recruitmentRepository.findById(r3Id).get().getInfoPost()).isNull(); // FK 정리 확인
        assertThat(recruitmentApplicationRepository.count()).isEqualTo(0);

        assertThat(infoPostRepository.count()).isEqualTo(0);
        assertThat(infoPostImageRepository.count()).isEqualTo(0);

        assertThat(teamInvitationRepository.count()).isEqualTo(0);

        assertThat(voteRepository.count()).isEqualTo(1); // 투표 자체는 leader 소유라 유지
        assertThat(voteParticipantRepository.count()).isEqualTo(0);
        assertThat(voteAvailabilityRepository.count()).isEqualTo(0);
        assertThat(voteDateRepository.count()).isEqualTo(1);
        assertThat(voteTimeSlotRepository.count()).isEqualTo(1);

        assertThat(teamNoticeRepository.count()).isEqualTo(0);
        assertThat(teamNoticeImageRepository.count()).isEqualTo(0);
        assertThat(teamNoticeReadRepository.count()).isEqualTo(0);

        assertThat(eventRepository.findById(teamEventId)).isPresent(); // team이 있어 삭제 대상 아님
        assertThat(eventRepository.count()).isEqualTo(1); // personalEvent만 삭제됨
        assertThat(eventParticipantRepository.count()).isEqualTo(0);
        assertThat(recurrenceExceptionParticipantRepository.count()).isEqualTo(0);
        assertThat(recurrenceExceptionRepository.count()).isEqualTo(1); // teamEvent용 exception만 남음
        assertThat(recurrenceRuleRepository.count()).isEqualTo(0);

        assertThat(fcmTokenRepository.count()).isEqualTo(0);

        assertThat(notificationRepository.count()).isEqualTo(1); // leader가 받은 알림은 유지
        assertThat(refreshTokenRepository.findByUserId(targetId)).isEmpty();

        assertThat(chatRoomRepository.findById(teamChatRoomId)).isPresent();
        assertThat(chatRoomRepository.findById(directChatRoomId)).isPresent();
        assertThat(chatRoomMemberRepository.count()).isEqualTo(2); // leader-teamRoom, other-directRoom만 남음
    }

    @Test
    @DisplayName("팀장으로 있는 팀이 있으면 계정을 삭제할 수 없다")
    void deleteUser_throwsWhenUserIsTeamLeader() {
        User leader = createUser("target2", "target2@inu.ac.kr");
        actingAs(leader);
        Team team = teamRepository.save(
                Team.builder().name("team2").description("desc").category(Category.PROJECT).build());
        teamMemberRepository.save(TeamMember.create(team, leader, TeamRole.LEADER));
        entityManager.flush();

        RestApiException exception = assertThrows(RestApiException.class,
                () -> userService.deleteUser(leader));

        assertThat(exception.getErrorCode()).isEqualTo(CustomErrorCode.TEAM_MEMBER_IS_HOST);
        assertThat(userRepository.findById(leader.getUserId())).isPresent();
    }

    @Test
    @DisplayName("본인이 생성한 진행 중인 투표가 있으면 계정을 삭제할 수 없다")
    void deleteUser_throwsWhenUserHasOpenVote() {
        User target = createUser("target3", "target3@inu.ac.kr");
        User leader = createUser("leader3", "leader3@inu.ac.kr");

        actingAs(leader);
        Team team = teamRepository.save(
                Team.builder().name("team3").description("desc").category(Category.PROJECT).build());
        teamMemberRepository.save(TeamMember.create(team, leader, TeamRole.LEADER));

        actingAs(target);
        teamMemberRepository.save(TeamMember.create(team, target, TeamRole.MEMBER));
        voteRepository.save(Vote.builder()
                .team(team).title("진행중 투표").description("설명").isOpened(true).isAllDay(false)
                .dailyTimeStart(LocalTime.of(9, 0)).dailyTimeEnd(LocalTime.of(20, 0))
                .slotUnitMinute(30).build());
        entityManager.flush();

        RestApiException exception = assertThrows(RestApiException.class,
                () -> userService.deleteUser(target));

        assertThat(exception.getErrorCode()).isEqualTo(CustomErrorCode.VOTE_IS_OPEN);
        assertThat(userRepository.findById(target.getUserId())).isPresent();
    }

    @Test
    @DisplayName("본인이 생성한 종료된 투표가 있으면 투표의 모든 연관 데이터와 함께 계정이 삭제된다")
    void deleteUser_deletesClosedVoteCreatedByUser() {
        User target = createUser("target4", "target4@inu.ac.kr");
        User leader = createUser("leader4", "leader4@inu.ac.kr");

        actingAs(leader);
        Team team = teamRepository.save(
                Team.builder().name("team4").description("desc").category(Category.PROJECT).build());
        TeamMember leaderMember = teamMemberRepository.save(
                TeamMember.create(team, leader, TeamRole.LEADER));

        actingAs(target);
        TeamMember targetMember = teamMemberRepository.save(
                TeamMember.create(team, target, TeamRole.MEMBER));
        Vote closedVote = voteRepository.save(Vote.builder()
                .team(team).title("종료된 투표").description("설명").isOpened(false).isAllDay(false)
                .dailyTimeStart(LocalTime.of(9, 0)).dailyTimeEnd(LocalTime.of(20, 0))
                .slotUnitMinute(30).build());
        VoteDate voteDate = voteDateRepository.save(
                VoteDate.create(closedVote, LocalDate.now().plusDays(1)));
        VoteTimeSlot voteTimeSlot = voteTimeSlotRepository.save(
                VoteTimeSlot.create(voteDate, LocalTime.of(9, 0), LocalTime.of(9, 30)));

        VoteParticipant targetParticipant = voteParticipantRepository.save(
                VoteParticipant.create(closedVote, targetMember));
        VoteParticipant leaderParticipant = voteParticipantRepository.save(
                VoteParticipant.create(closedVote, leaderMember));
        voteAvailabilityRepository.save(
                VoteAvailability.create(targetParticipant, voteTimeSlot));
        voteAvailabilityRepository.save(
                VoteAvailability.create(leaderParticipant, voteTimeSlot));

        entityManager.flush();
        entityManager.clear();

        Long targetId = target.getUserId();
        Long leaderId = leader.getUserId();
        Long teamId = team.getTeamId();
        Long closedVoteId = closedVote.getVoteId();

        userService.deleteUser(userRepository.getReferenceById(targetId));
        entityManager.flush();
        entityManager.clear();

        assertThat(userRepository.findById(targetId)).isEmpty();
        assertThat(userRepository.findById(leaderId)).isPresent();
        assertThat(teamRepository.findById(teamId)).isPresent();
        assertThat(teamMemberRepository.findByTeamAndUser(
                teamRepository.getReferenceById(teamId),
                userRepository.getReferenceById(leaderId)
        )).isPresent();

        assertThat(voteRepository.findById(closedVoteId)).isEmpty();
        assertThat(voteParticipantRepository.count()).isZero();
        assertThat(voteAvailabilityRepository.count()).isZero();
        assertThat(voteDateRepository.count()).isZero();
        assertThat(voteTimeSlotRepository.count()).isZero();
    }

    private User createUser(String username, String email) {
        return userRepository.save(User.builder()
                .username(username)
                .email(email)
                .password("encoded-password")
                .name(username)
                .department(Department.COMPUTER_SCIENCE)
                .studentNumber(null)
                .isSchoolVerified(false)
                .role(Role.USER)
                .imageKey(null)
                .build());
    }

    private void actingAs(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new UserDetailsImpl(user), null, new UserDetailsImpl(user).getAuthorities()));
    }
}
