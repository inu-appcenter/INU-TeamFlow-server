package com.inuteamflow.server.domain.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inuteamflow.server.domain.event.dto.request.MyEventUpdateRequest;
import com.inuteamflow.server.domain.event.entity.Event;
import com.inuteamflow.server.domain.event.entity.RecurrenceRule;
import com.inuteamflow.server.domain.event.enums.EventColor;
import com.inuteamflow.server.domain.event.enums.RecurrenceFrequency;
import com.inuteamflow.server.domain.event.repository.EventRepository;
import com.inuteamflow.server.domain.event.repository.RecurrenceRuleRepository;
import com.inuteamflow.server.domain.event.service.MyEventService;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import com.inuteamflow.server.domain.user.enums.Department;
import com.inuteamflow.server.domain.user.enums.Role;
import com.inuteamflow.server.domain.user.repository.UserRepository;
import com.inuteamflow.server.global.s3.S3Service;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * {@link MyEventService#updateMyEvent}의 반복 일정 전체 수정이 실제 트랜잭션 커밋 후에도 반영되는지 검증한다.
 * - 픽스처 생성, 일정 수정, 결과 조회를 각각 독립된 트랜잭션으로 실행한다.
 * - 반복 예외 참여자 벌크 삭제가 영속성 컨텍스트를 비우면서 수정 내용이 사라지지 않는지 확인한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MyEventUpdateCommitTest {

    @Autowired
    private MyEventService myEventService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private S3Service s3Service;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private RecurrenceRuleRepository recurrenceRuleRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("반복 일정 전체 수정 시 일정과 반복 규칙 변경이 커밋 후에도 유지된다")
    void updateMyEvent_allSeries_commitsEventAndRuleChanges() throws Exception {
        LocalDateTime start = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(10, 0));
        FixtureIds ids = transactionTemplate.execute(status -> createFixture(start));
        assertThat(ids).isNotNull();

        User user = transactionTemplate.execute(
                status -> userRepository.findById(ids.userId()).orElseThrow());
        assertThat(user).isNotNull();
        actingAs(user);

        // 컨트롤러와 동일하게 JSON에서 요청 객체를 만든다.
        MyEventUpdateRequest request =
                objectMapper.readValue("""
                {
                  "title": "바뀐 제목",
                  "startAt": "%s",
                  "endAt": "%s",
                  "isAllDay": false,
                  "color": "LEAF",
                  "isFinished": false,
                  "recurrenceEditScope": "ALL_SERIES",
                  "recurrence": {"freq": "WEEKLY", "intervalValue": 2, "byDay": ["WEDNESDAY"]}
                }
                """.formatted(start, start.plusHours(2)), MyEventUpdateRequest.class);

        // 테스트 트랜잭션이 없는 상태에서 호출하므로 메서드 반환 전에 서비스 트랜잭션이 커밋된다.
        myEventService.updateMyEvent(user, ids.eventId(), request);

        transactionTemplate.executeWithoutResult(status -> {
            Event event = eventRepository.findById(ids.eventId()).orElseThrow();
            assertThat(event.getTitle()).isEqualTo("바뀐 제목");
            assertThat(event.getEndAt()).isEqualTo(start.plusHours(2));

            RecurrenceRule rule =
                    recurrenceRuleRepository.findById(ids.recurrenceRuleId()).orElseThrow();
            assertThat(rule.getIntervalValue()).isEqualTo(2);
            assertThat(rule.getByDay()).containsExactly(DayOfWeek.WEDNESDAY);
        });
    }

    private FixtureIds createFixture(LocalDateTime start) {
        User user = userRepository.save(User.builder()
                .username("update-commit-user")
                .email("update-commit-user@inu.ac.kr")
                .password("encoded-password")
                .name("update-commit-user")
                .department(Department.COMPUTER_SCIENCE)
                .studentNumber(null)
                .isSchoolVerified(false)
                .role(Role.USER)
                .imageKey(null)
                .build());

        actingAs(user);
        Event event = eventRepository.save(Event.builder()
                .title("원래 제목")
                .description("desc")
                .startAt(start)
                .endAt(start.plusHours(1))
                .isAllDay(false)
                .color(EventColor.OCEAN)
                .uid(UUID.randomUUID().toString())
                .sequence(0)
                .isFinished(false)
                .isSingle(false)
                .build());
        RecurrenceRule rule = recurrenceRuleRepository.save(RecurrenceRule.builder()
                .event(event)
                .freq(RecurrenceFrequency.WEEKLY)
                .intervalValue(1)
                .byDay(List.of(DayOfWeek.MONDAY))
                .seriesStartAt(start)
                .build());

        return new FixtureIds(user.getUserId(), event.getEventId(), rule.getRecurrenceRuleId());
    }

    private void actingAs(User user) {
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }

    private record FixtureIds(Long userId, Long eventId, Long recurrenceRuleId) {}
}
