package com.inuteamflow.server.domain.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inuteamflow.server.domain.admin.service.AdminService;
import com.inuteamflow.server.domain.inquiry.dto.request.InquiryHandleRequest;
import com.inuteamflow.server.domain.inquiry.dto.response.InquiryDetailResponse;
import com.inuteamflow.server.domain.inquiry.entity.Inquiry;
import com.inuteamflow.server.domain.inquiry.enums.InquiryStatus;
import com.inuteamflow.server.domain.inquiry.enums.InquiryType;
import com.inuteamflow.server.domain.inquiry.repository.InquiryRepository;
import com.inuteamflow.server.domain.inquiry.service.InquiryService;
import com.inuteamflow.server.domain.notification.service.NotificationService;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.enums.Department;
import com.inuteamflow.server.domain.user.enums.Role;
import com.inuteamflow.server.domain.user.repository.UserRepository;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import com.inuteamflow.server.global.s3.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * 문의 처리 흐름이 제대로 동작하는지 검증한다.
 *
 * <p>문의 처리는 답변 내용과 답변자를 기록하고 상태를 답변 완료로 바꾸는 과정이다.
 * 관리자는 문의자 본인이 아니어도 모든 문의를 조회하고 답변할 수 있어야 하며, 이미 답변된 문의는 다시 답변할 수 없다.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InquiryAnswerTest {

    @Autowired
    private AdminService adminService;

    @Autowired
    private InquiryService inquiryService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InquiryRepository inquiryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private S3Service s3Service;

    @MockitoBean
    private NotificationService notificationService;

    private User admin;
    private User inquirer;

    @BeforeEach
    void setUp() {
        admin = saveUser("answer-admin", Role.ADMIN);
        inquirer = saveUser("answer-inquirer", Role.USER);
    }

    @Test
    @DisplayName("문의에 답변하면 답변 내용과 답변자, 답변 시각이 기록되고 상태가 답변 완료로 바뀐다.")
    void answerInquiry_recordsAnswerAndAnswerer() throws JsonProcessingException {
        Inquiry inquiry = saveInquiry("알림이 중복으로 와요");

        adminService.handleInquiry(inquiry.getInquiryId(), answerRequest("수정했습니다."), admin);

        assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.RESOLVED);
        assertThat(inquiry.getAnswer()).isEqualTo("수정했습니다.");
        assertThat(inquiry.getAnswererId()).isEqualTo(admin.getUserId());
        assertThat(inquiry.getAnswererName()).isEqualTo(admin.getName());
        assertThat(inquiry.getAnsweredAt()).isNotNull();
    }

    @Test
    @DisplayName("답변 전 문의의 상세에는 답변 정보가 담기지 않는다.")
    void pendingInquiry_hasNoAnswerInfo() {
        Inquiry inquiry = saveInquiry("답변 전 문의");

        InquiryDetailResponse response = adminService.getInquiry(inquiry.getInquiryId(), admin);

        assertThat(response.getStatus()).isEqualTo(InquiryStatus.PENDING);
        assertThat(response.getAnswer()).isNull();
        assertThat(response.getAnsweredBy()).isNull();
        assertThat(response.getAnsweredAt()).isNull();
    }

    @Test
    @DisplayName("답변한 문의의 상세에는 답변 내용과 답변자가 담긴다.")
    void answeredInquiry_detailContainsAnswer() throws JsonProcessingException {
        Inquiry inquiry = saveInquiry("답변될 문의");
        adminService.handleInquiry(inquiry.getInquiryId(), answerRequest("처리했습니다."), admin);

        InquiryDetailResponse response = adminService.getInquiry(inquiry.getInquiryId(), admin);

        assertThat(response.getStatus()).isEqualTo(InquiryStatus.RESOLVED);
        assertThat(response.getAnswer()).isEqualTo("처리했습니다.");
        assertThat(response.getAnsweredBy().getUserId()).isEqualTo(admin.getUserId());
        assertThat(response.getAnsweredAt()).isNotNull();
    }

    @Test
    @DisplayName("관리자는 본인이 등록하지 않은 문의도 조회할 수 있다.")
    void adminCanReadOthersInquiry() {
        Inquiry inquiry = saveInquiry("타인의 문의");

        InquiryDetailResponse response = adminService.getInquiry(inquiry.getInquiryId(), admin);

        assertThat(response.getInquiryId()).isEqualTo(inquiry.getInquiryId());
        assertThat(response.getInquirer().getUserId()).isEqualTo(inquirer.getUserId());
    }

    @Test
    @DisplayName("일반 사용자는 본인이 등록하지 않은 문의를 조회할 수 없다.")
    void nonOwnerCannotReadOthersInquiry() {
        Inquiry inquiry = saveInquiry("타인의 문의");

        assertThatThrownBy(() -> inquiryService.getMyInquiry(inquiry.getInquiryId(), admin))
                .isInstanceOf(RestApiException.class)
                .extracting(exception -> ((RestApiException) exception).getErrorCode())
                .isEqualTo(CustomErrorCode.INQUIRY_FORBIDDEN);
    }

    @Test
    @DisplayName("이미 답변된 문의에는 다시 답변할 수 없다.")
    void alreadyAnsweredInquiry_throws() throws JsonProcessingException {
        Inquiry inquiry = saveInquiry("중복 답변 문의");
        adminService.handleInquiry(inquiry.getInquiryId(), answerRequest("첫 답변"), admin);

        assertThatThrownBy(() -> adminService.handleInquiry(inquiry.getInquiryId(), answerRequest("두 번째 답변"), admin))
                .isInstanceOf(RestApiException.class)
                .extracting(exception -> ((RestApiException) exception).getErrorCode())
                .isEqualTo(CustomErrorCode.INQUIRY_ALREADY_ANSWERED);
        assertThat(inquiry.getAnswer()).isEqualTo("첫 답변");
    }

    @Test
    @DisplayName("존재하지 않는 문의에는 답변할 수 없다.")
    void unknownInquiry_throws() throws JsonProcessingException {
        assertThatThrownBy(() -> adminService.handleInquiry(999_999L, answerRequest("답변"), admin))
                .isInstanceOf(RestApiException.class)
                .extracting(exception -> ((RestApiException) exception).getErrorCode())
                .isEqualTo(CustomErrorCode.INQUIRY_NOT_FOUND);
    }

    private Inquiry saveInquiry(String detail) {
        return inquiryRepository.saveAndFlush(
                Inquiry.create(inquirer.getUserId(), inquirer.getName(), InquiryType.BUG, detail));
    }

    private InquiryHandleRequest answerRequest(String answer) throws JsonProcessingException {
        return objectMapper.readValue("""
                { "answer": "%s" }
                """.formatted(answer), InquiryHandleRequest.class);
    }

    private User saveUser(String username, Role role) {
        return userRepository.save(User.builder()
                .username(username)
                .email(username + "@inu.ac.kr")
                .password("encoded-password")
                .name(username)
                .department(Department.COMPUTER_SCIENCE)
                .isSchoolVerified(false)
                .role(role)
                .build());
    }
}
