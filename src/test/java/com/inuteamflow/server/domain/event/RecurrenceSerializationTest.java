package com.inuteamflow.server.domain.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inuteamflow.server.domain.event.dto.response.EventListResponse;
import com.inuteamflow.server.domain.event.entity.Event;
import com.inuteamflow.server.domain.event.entity.RecurrenceRule;
import com.inuteamflow.server.domain.event.enums.EventColor;
import com.inuteamflow.server.domain.event.enums.RecurrenceFrequency;
import com.inuteamflow.server.domain.event.repository.EventRepository;
import com.inuteamflow.server.domain.event.repository.RecurrenceRuleRepository;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import com.inuteamflow.server.domain.user.enums.Department;
import com.inuteamflow.server.domain.user.enums.Role;
import com.inuteamflow.server.domain.user.repository.UserRepository;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * {@link EventListResponse} 직렬화 시 {@link RecurrenceRule#getByDay()}(LAZY @ElementCollection)로 인한
 * {@link org.hibernate.LazyInitializationException} 회귀를 검증한다.
 *
 * <p>이 버그는 트랜잭션(=Hibernate 세션) 안에서 DTO를 만들고, 세션이 닫힌 뒤 Jackson이 직렬화할 때만 재현된다.
 * 따라서 이 테스트는 클래스 단위 {@code @Transactional}을 쓰지 않고 {@link TransactionTemplate}으로
 * 트랜잭션 경계를 직접 제어하여 실제 요청 처리 흐름(트랜잭션 안 DTO 생성 → 트랜잭션 밖 직렬화)을 재현한다.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class RecurrenceSerializationTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 5, 20, 18, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 5, 20, 20, 0);

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private RecurrenceRuleRepository recurrenceRuleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate txTemplate;

    @BeforeEach
    void setUp() {
        txTemplate = new TransactionTemplate(transactionManager);

        // @CreatedBy 감사 컬럼(created_by, NOT NULL)이 채워지도록 인증 사용자를 등록한다.
        User author = userRepository.save(User.builder()
                .username("event-author")
                .email("event-author@inu.ac.kr")
                .password("encoded-password")
                .name("event-author")
                .department(Department.COMPUTER_SCIENCE)
                .isSchoolVerified(false)
                .role(Role.USER)
                .build());
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(new UserDetailsImpl(author), null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        // 클래스 단위 @Transactional을 쓰지 않아 커밋된 데이터가 남으므로 직접 정리한다. (FK 때문에 rule 먼저 삭제)
        recurrenceRuleRepository.deleteAllInBatch();
        eventRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("byDay가 비어있는 비주간(DAILY) 반복 일정을 세션 종료 후 직렬화해도 LazyInitializationException이 발생하지 않는다")
    void serializeNonWeeklyRecurrence_afterSessionClosed_doesNotThrow() {
        Long eventId = persistRecurringEvent(RecurrenceFrequency.DAILY, List.of());

        EventListResponse response = buildResponseWithinTransaction(eventId);

        assertThatCode(() -> objectMapper.writeValueAsString(response)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("주간(WEEKLY) 반복 일정을 세션 종료 후 직렬화하면 byDay 값이 유지된다")
    void serializeWeeklyRecurrence_afterSessionClosed_preservesByDay() throws Exception {
        Long eventId =
                persistRecurringEvent(RecurrenceFrequency.WEEKLY, List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY));

        EventListResponse response = buildResponseWithinTransaction(eventId);

        String json = objectMapper.writeValueAsString(response);
        assertThat(json).contains("MONDAY").contains("WEDNESDAY");
    }

    /**
     * 트랜잭션(=세션) 안에서 응답 DTO를 만들고 반환한다. 반환 이후에는 세션이 닫힌 상태이므로,
     * 이후 직렬화 시점에 LAZY 컬렉션이 초기화되어 있지 않으면 LazyInitializationException이 발생한다.
     */
    private EventListResponse buildResponseWithinTransaction(Long eventId) {
        return txTemplate.execute(status -> {
            Event event = eventRepository.findById(eventId).orElseThrow();
            RecurrenceRule rule = recurrenceRuleRepository.findByEvent(event).orElseThrow();
            return EventListResponse.of(event, rule, START, START, END, false, List.of());
        });
    }

    private Long persistRecurringEvent(RecurrenceFrequency freq, List<DayOfWeek> byDay) {
        return txTemplate.execute(status -> {
            Event event = eventRepository.save(Event.builder()
                    .title("직렬화 검증용 반복 일정")
                    .description("반복 일정 직렬화 회귀 테스트")
                    .startAt(START)
                    .endAt(END)
                    .isAllDay(false)
                    .color(EventColor.MINT)
                    .uid(UUID.randomUUID().toString())
                    .sequence(0)
                    .isFinished(false)
                    .isSingle(false)
                    .build());

            recurrenceRuleRepository.save(RecurrenceRule.builder()
                    .event(event)
                    .freq(freq)
                    .intervalValue(1)
                    .byDay(byDay)
                    .seriesStartAt(START)
                    .build());

            return event.getEventId();
        });
    }
}
