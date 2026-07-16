package com.inuteamflow.server.global.exception.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CustomErrorCode implements ErrorCode {

    // 유저 관련 에러
    AUTH_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, 401, "아이디 또는 비밀번호가 올바르지 않습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "사용자를 찾을 수 없습니다."),
    USER_BANNED(HttpStatus.FORBIDDEN, 403, "정지된 사용자입니다."),
    USER_USERNAME_CONFLICT(HttpStatus.CONFLICT, 409, "이미 사용 중인 아이디입니다."),
    USER_EMAIL_CONFLICT(HttpStatus.CONFLICT, 409, "이미 사용 중인 이메일입니다."),
    USER_SCHOOL_VERIFY_FAILED(HttpStatus.BAD_REQUEST, 400, "학교 인증에 실패했습니다."),
    USER_SCHOOL_VERIFY_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, 503, "학교 인증 서비스를 현재 사용할 수 없습니다."),
    USER_SCHOOL_VERIFICATION_REQUIRED(HttpStatus.FORBIDDEN, 403, "학교 인증이 필요한 기능입니다."),
    USER_STUDENT_NUMBER_CONFLICT(HttpStatus.CONFLICT, 409, "이미 다른 계정에 연결된 학번입니다."),
    USER_SCHOOL_ALREADY_VERIFIED(HttpStatus.CONFLICT, 409, "이미 학교 인증이 된 계정입니다."),

    // JWT 인증 에러
    JWT_INVALID(HttpStatus.UNAUTHORIZED, 401, ""),
    JWT_EXPIRED(HttpStatus.UNAUTHORIZED, 401, ""),
    JWT_MALFORMED(HttpStatus.UNAUTHORIZED, 401, ""),
    JWT_UNSUPPORTED(HttpStatus.UNAUTHORIZED, 401, ""),
    JWT_REFRESH_NOT_MATCH(HttpStatus.BAD_REQUEST, 400, ""),
    JWT_REFRESH_NOT_FOUND(HttpStatus.NOT_FOUND, 404, ""),

    // 요청 관련 에러
    COMMON_INVALID_REQUEST(HttpStatus.BAD_REQUEST, 400, ""),
    COMMON_HANDLER_NOT_FOUND(HttpStatus.NOT_FOUND, 404, ""),

    // 팀 관련 에러
    TEAM_NOT_FOUND(HttpStatus.NOT_FOUND, 404, ""),
    TEAM_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, 404, ""),
    TEAM_FORBIDDEN(HttpStatus.FORBIDDEN, 403, ""),
    TEAM_MEMBER_ALREADY_ROLE(HttpStatus.BAD_REQUEST, 400, ""),
    TEAM_MEMBER_IS_HOST(HttpStatus.FORBIDDEN, 403, "사용자가 팀장으로 있는 팀이 존재합니다"),
    TEAM_CANNOT_KICK_SELF(HttpStatus.BAD_REQUEST, 400, "자기 자신은 방출할 수 없습니다."),

    // 팀 초대 관련 에러
    INVITATION_FORBIDDEN(HttpStatus.FORBIDDEN, 403, "멤버 초대 권한이 없습니다."),
    INVITATION_RECEIVER_NOT_VERIFIED(HttpStatus.BAD_REQUEST, 400, "학번 인증이 되지 않은 사용자입니다."),
    INVITATION_ALREADY_MEMBER(HttpStatus.BAD_REQUEST, 400, "이미 멤버인 사용자입니다."),
    INVITATION_ALREADY_SENT(HttpStatus.BAD_REQUEST, 400, "이미 초대를 보낸 사용자입니다."),
    INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "초대를 찾을 수 없습니다."),
    INVITATION_STATUS_INVALID(HttpStatus.BAD_REQUEST, 400, "초대의 상태가 유효하지 않습니다"),
    INVITATION_SELF_INVITE(HttpStatus.BAD_REQUEST, 400, "자기 자신은 초대할 수 없습니다."),

    // 모집글 관련 에러
    RECRUITMENT_MEMBER_FULL(HttpStatus.CONFLICT, 409, "정원이 가득 찼습니다."),
    RECRUITMENT_APPLICATION_STATUS_INVALID(HttpStatus.BAD_REQUEST, 400, "유효하지 않은 신청서 상태입니다."),
    RECRUITMENT_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "모집글을 찾을 수 없습니다."),
    RECRUITMENT_FORBIDDEN(HttpStatus.FORBIDDEN, 403, "모집글에 대한 권한이 없습니다."),
    RECRUITMENT_ALREADY_APPLIED(HttpStatus.CONFLICT, 409, "이미 신청한 모집글입니다."),
    RECRUITMENT_APPLICANT_FORBIDDEN(HttpStatus.FORBIDDEN, 403, "본인 모집글에는 신청할 수 없습니다."),
    RECRUITMENT_APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "신청서를 찾을 수 없습니다."),
    RECRUITMENT_CLOSED(HttpStatus.CONFLICT, 409, "이미 마감된 모집글입니다."),
    RECRUITMENT_EXPIRED(HttpStatus.CONFLICT, 409, "모집글의 신청기간이 지났습니다."),

    // 정보글 관련 에러
    INFO_POST_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "공고를 찾을 수 없습니다."),
    INFO_POST_FORBIDDEN(HttpStatus.FORBIDDEN, 403, "공고에 권한이 없습니다."),

    // 투표 관련 에러
    VOTE_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "투표를 찾을 수 없습니다."),
    VOTE_NOT_OPENED(HttpStatus.BAD_REQUEST, 400, "열려 있는 투표가 아닙니다."),
    VOTE_IS_OPEN(HttpStatus.FORBIDDEN, 403, "진행 중인 투표가 존재합니다."),
    VOTE_PARTICIPANT_INVALID(HttpStatus.BAD_REQUEST, 400, "유효하지 않은 투표 참여자입니다."),
    VOTE_PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "해당 투표의 참여자를 찾을 수 없습니다."),
    VOTE_DATE_INVALID(HttpStatus.BAD_REQUEST, 400, "투표 날짜 범위가 올바르지 않습니다."),
    VOTE_TIME_INVALID(HttpStatus.BAD_REQUEST, 400, "투표 시간 범위가 올바르지 않습니다."),
    VOTE_TIME_SLOT_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "투표 시간 슬롯을 찾을 수 없습니다."),
    VOTE_TIME_SLOT_INVALID(HttpStatus.BAD_REQUEST, 400, "유효하지 않은 투표 시간 슬롯입니다."),
    VOTE_RESULT_ALREADY_EXISTS(HttpStatus.CONFLICT, 409, "이미 투표 결과가 확정되었습니다."),
    VOTE_RESULT_TIME_INVALID(HttpStatus.BAD_REQUEST, 400, "투표 결과 확정 시간이 올바르지 않습니다."),

    // 팀 공지 관련 에러
    TEAM_NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "공지를 찾을 수 없습니다."),
    TEAM_NOTICE_FORBIDDEN(HttpStatus.FORBIDDEN, 403, "공지에 대한 권한이 없습니다."),

    // 일정 관련 에러
    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "일정을 찾을 수 없습니다."),
    EVENT_FORBIDDEN(HttpStatus.FORBIDDEN, 403, "해당 일정에 대한 권한이 없습니다."),
    EVENT_TEAM_MISMATCH(HttpStatus.BAD_REQUEST, 400, "요청한 팀과 일정의 소속 팀이 일치하지 않습니다."),
    EVENT_MONTH_INVALID(HttpStatus.BAD_REQUEST, 400, "조회할 연도 또는 월 정보가 올바르지 않습니다."),
    EVENT_PARTICIPANT_INVALID(HttpStatus.BAD_REQUEST, 400, "유효하지 않은 일정 참여자입니다."),
    EVENT_PARTICIPANT_HOST_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "일정의 주최자를 찾을 수 없습니다."),
    EVENT_RECURRENCE_RULE_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "반복 일정 규칙을 찾을 수 없습니다."),
    EVENT_RECURRENCE_REQUIRED(HttpStatus.BAD_REQUEST, 400, "반복 일정 정보가 필요합니다."),
    EVENT_RECURRENCE_OCCURRENCE_REQUIRED(HttpStatus.BAD_REQUEST, 400, "반복 일정 발생 시점(occurrenceAt)이 필요합니다."),
    EVENT_RECURRENCE_OCCURRENCE_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "해당 반복 일정 발생 시점을 찾을 수 없습니다."),

    // 채팅 관련 에러
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "채팅방을 찾을 수 없습니다."),
    CHAT_ROOM_FORBIDDEN(HttpStatus.FORBIDDEN, 403, "채팅방 멤버가 아닙니다."),
    CHAT_MESSAGE_TYPE_INVALID(HttpStatus.BAD_REQUEST, 400, "잘못된 메시지 타입입니다."),
    CHAT_ROOM_INVALID_TARGET(HttpStatus.BAD_REQUEST, 400, "자기 자신과는 1:1 채팅을 할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final Integer code;
    private final String message;
}
