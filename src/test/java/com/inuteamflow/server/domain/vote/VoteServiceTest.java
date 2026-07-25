package com.inuteamflow.server.domain.vote;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inuteamflow.server.domain.event.entity.Event;
import com.inuteamflow.server.domain.event.enums.EventColor;
import com.inuteamflow.server.domain.event.repository.EventRepository;
import com.inuteamflow.server.domain.team.entity.Team;
import com.inuteamflow.server.domain.team.entity.TeamMember;
import com.inuteamflow.server.domain.team.enums.TeamRole;
import com.inuteamflow.server.domain.team.repository.TeamMemberRepository;
import com.inuteamflow.server.domain.team.repository.TeamRepository;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import com.inuteamflow.server.domain.user.enums.Department;
import com.inuteamflow.server.domain.user.enums.Role;
import com.inuteamflow.server.domain.user.repository.UserRepository;
import com.inuteamflow.server.domain.vote.dto.request.EventVoteCreateRequest;
import com.inuteamflow.server.domain.vote.entity.Vote;
import com.inuteamflow.server.domain.vote.entity.VoteAvailability;
import com.inuteamflow.server.domain.vote.entity.VoteDate;
import com.inuteamflow.server.domain.vote.entity.VoteParticipant;
import com.inuteamflow.server.domain.vote.entity.VoteResult;
import com.inuteamflow.server.domain.vote.entity.VoteTimeSlot;
import com.inuteamflow.server.domain.vote.repository.VoteAvailabilityRepository;
import com.inuteamflow.server.domain.vote.repository.VoteDateRepository;
import com.inuteamflow.server.domain.vote.repository.VoteParticipantRepository;
import com.inuteamflow.server.domain.vote.repository.VoteRepository;
import com.inuteamflow.server.domain.vote.repository.VoteResultRepository;
import com.inuteamflow.server.domain.vote.repository.VoteTimeSlotRepository;
import com.inuteamflow.server.domain.vote.service.VoteParticipantService;
import com.inuteamflow.server.domain.vote.service.VoteService;
import com.inuteamflow.server.global.enums.Category;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;

/**
 * {@link VoteService}의 투표 삭제 기능을 실제 Repository와 H2 데이터베이스로 검증한다.
 * - 투표 삭제 시 참여자, 가능 시간, 시간 슬롯, 날짜, 결과 등 모든 종속 데이터가 삭제되는지 확인한다.
 * - 확정된 투표를 삭제해도 투표 결과로 생성된 일정은 유지되는지 확인한다.
 * - 투표 생성자가 아닌 일반 팀원의 삭제 요청이 거부되는지 확인한다.
 * - 연관 데이터 삭제 중 예외가 발생하면 전체 삭제 작업이 롤백되는지 확인한다.
 * - 각 테스트는 독립된 서비스 트랜잭션을 사용하며, 공통 데이터는 테스트 종료 후 명시적으로 정리한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class VoteServiceTest {

    @Autowired private VoteService voteService;
    @Autowired private UserRepository userRepository;
    @Autowired private TeamRepository teamRepository;
    @Autowired private TeamMemberRepository teamMemberRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private VoteRepository voteRepository;
    @Autowired private VoteDateRepository voteDateRepository;
    @Autowired private VoteTimeSlotRepository voteTimeSlotRepository;
    @Autowired private VoteParticipantRepository voteParticipantRepository;
    @Autowired private VoteAvailabilityRepository voteAvailabilityRepository;
    @Autowired private VoteResultRepository voteResultRepository;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TransactionTemplate transactionTemplate;

    @MockitoSpyBean
    private VoteParticipantService voteParticipantService;

    private User creator;
    private User regularMember;
    private Long voteId;
    private Long eventId;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        creator = saveUser("vote-creator");
        regularMember = saveUser("vote-regular-member");
        actingAs(creator);

        EventVoteCreateRequest request = objectMapper.readValue("""
                {
                  "title": "삭제 테스트 투표",
                  "description": "투표 연관 데이터 삭제를 검증한다.",
                  "participants": [],
                  "isAllDay": false,
                  "dates": ["2026-07-27"],
                  "dailyTimeStart": "10:00:00",
                  "dailyTimeEnd": "11:00:00"
                }
                """, EventVoteCreateRequest.class);

        transactionTemplate.executeWithoutResult(status -> {
            Team team = teamRepository.save(Team.builder()
                    .name("투표 삭제 테스트 팀")
                    .description("VoteService 삭제 테스트")
                    .category(Category.PROJECT)
                    .build());
            TeamMember creatorMember = teamMemberRepository.save(
                    TeamMember.create(team, creator, TeamRole.MEMBER)
            );
            TeamMember regularTeamMember = teamMemberRepository.save(
                    TeamMember.create(team, regularMember, TeamRole.MEMBER)
            );

            Vote vote = voteRepository.save(Vote.create(team, request));
            VoteDate voteDate = voteDateRepository.save(
                    VoteDate.create(vote, request.getDates().get(0))
            );
            VoteTimeSlot firstSlot = voteTimeSlotRepository.save(
                    VoteTimeSlot.create(
                            voteDate,
                            request.getDailyTimeStart(),
                            request.getDailyTimeStart().plusMinutes(30)
                    )
            );
            VoteTimeSlot secondSlot = voteTimeSlotRepository.save(
                    VoteTimeSlot.create(
                            voteDate,
                            request.getDailyTimeStart().plusMinutes(30),
                            request.getDailyTimeEnd()
                    )
            );
            VoteParticipant creatorParticipant = voteParticipantRepository.save(
                    VoteParticipant.create(vote, creatorMember)
            );
            VoteParticipant regularParticipant = voteParticipantRepository.save(
                    VoteParticipant.create(vote, regularTeamMember)
            );
            voteAvailabilityRepository.save(
                    VoteAvailability.create(creatorParticipant, firstSlot)
            );
            voteAvailabilityRepository.save(
                    VoteAvailability.create(regularParticipant, secondSlot)
            );

            Event event = eventRepository.save(Event.builder()
                    .team(team)
                    .title("투표로 확정된 일정")
                    .description("투표 삭제 후에도 유지되어야 한다.")
                    .startAt(LocalDateTime.of(2026, 7, 27, 10, 0))
                    .endAt(LocalDateTime.of(2026, 7, 27, 11, 0))
                    .isAllDay(false)
                    .color(EventColor.OCEAN)
                    .uid("vote-delete-test-event")
                    .sequence(0)
                    .isFinished(false)
                    .isSingle(true)
                    .build());
            voteResultRepository.save(VoteResult.create(
                    vote,
                    event,
                    false,
                    event.getStartAt(),
                    event.getEndAt()
            ));
            vote.close();

            voteId = vote.getVoteId();
            eventId = event.getEventId();
        });
    }

    @AfterEach
    void tearDown() {
        doCallRealMethod()
                .when(voteParticipantService)
                .deleteByVoteId(voteId);

        transactionTemplate.executeWithoutResult(status -> {
            voteAvailabilityRepository.deleteAllInBatch();
            voteResultRepository.deleteAllInBatch();
            voteParticipantRepository.deleteAllInBatch();
            voteTimeSlotRepository.deleteAllInBatch();
            voteDateRepository.deleteAllInBatch();
            voteRepository.deleteAllInBatch();
            eventRepository.deleteAllInBatch();
            teamMemberRepository.deleteAllInBatch();
            teamRepository.deleteAllInBatch();
            userRepository.deleteAllInBatch();
        });
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("투표를 삭제하면 모든 투표 연관 데이터가 삭제된다.")
    void deleteVote_deletesAllVoteAssociations() {
        voteService.deleteVote(creator, voteId);

        assertThat(voteRepository.existsById(voteId)).isFalse();
        assertThat(voteResultRepository.findAll()).isEmpty();
        assertThat(voteParticipantRepository.findAll()).isEmpty();
        assertThat(voteAvailabilityRepository.findAll()).isEmpty();
        assertThat(voteTimeSlotRepository.findAll()).isEmpty();
        assertThat(voteDateRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("확정된 투표를 삭제해도 투표로 생성된 일정은 유지된다.")
    void deleteConfirmedVote_keepsConfirmedEvent() {
        voteService.deleteVote(creator, voteId);

        assertThat(voteRepository.existsById(voteId)).isFalse();
        assertThat(voteResultRepository.findAll()).isEmpty();
        assertThat(eventRepository.existsById(eventId)).isTrue();
    }

    @Test
    @DisplayName("일반 팀원은 다른 사용자가 생성한 투표를 삭제할 수 없다.")
    void deleteVote_byRegularMember_throwsForbidden() {
        assertThatThrownBy(() -> voteService.deleteVote(regularMember, voteId))
                .isInstanceOf(RestApiException.class)
                .extracting(exception -> ((RestApiException) exception).getErrorCode())
                .isEqualTo(CustomErrorCode.VOTE_DELETE_FORBIDDEN);

        assertThat(voteRepository.existsById(voteId)).isTrue();
        assertThat(voteResultRepository.findAll()).hasSize(1);
        assertThat(voteParticipantRepository.findAll()).hasSize(2);
        assertThat(voteAvailabilityRepository.findAll()).hasSize(2);
        assertThat(voteTimeSlotRepository.findAll()).hasSize(2);
        assertThat(voteDateRepository.findAll()).hasSize(1);
        assertThat(eventRepository.existsById(eventId)).isTrue();
    }

    @Test
    @DisplayName("연관 데이터 삭제 중 예외가 발생하면 앞서 삭제한 데이터도 롤백된다.")
    void deleteVote_whenAssociationDeletionFails_rollsBackEntireTransaction() {
        doThrow(new IllegalStateException("투표 참여자 삭제 실패"))
                .when(voteParticipantService)
                .deleteByVoteId(voteId);

        assertThatThrownBy(() -> voteService.deleteVote(creator, voteId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("투표 참여자 삭제 실패");

        assertThat(voteRepository.existsById(voteId)).isTrue();
        assertThat(voteResultRepository.findAll()).hasSize(1);
        assertThat(voteParticipantRepository.findAll()).hasSize(2);
        assertThat(voteAvailabilityRepository.findAll()).hasSize(2);
        assertThat(voteTimeSlotRepository.findAll()).hasSize(2);
        assertThat(voteDateRepository.findAll()).hasSize(1);
        assertThat(eventRepository.existsById(eventId)).isTrue();
    }

    private User saveUser(String username) {
        return userRepository.save(User.builder()
                .username(username)
                .email(username + "@inu.ac.kr")
                .password("encoded-password")
                .name(username)
                .department(Department.COMPUTER_SCIENCE)
                .isSchoolVerified(false)
                .role(Role.USER)
                .build());
    }

    private void actingAs(User user) {
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                )
        );
    }
}
