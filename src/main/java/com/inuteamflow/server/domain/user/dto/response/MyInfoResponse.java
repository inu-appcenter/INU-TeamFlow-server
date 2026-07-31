package com.inuteamflow.server.domain.user.dto.response;

import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.enums.Department;
import com.inuteamflow.server.domain.user.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "내 정보 응답 DTO")
public class MyInfoResponse {

    @Schema(description = "유저 ID", example = "1")
    private Long userId;

    @Schema(description = "로그인 아이디", example = "appcenter123")
    private String username;

    @Schema(description = "이메일", example = "appcenter@inu.ac.kr")
    private String email;

    @Schema(description = "학번", example = "202201234")
    private String studentNumber;

    @Schema(description = "이름", example = "홍길동")
    private String name;

    @Schema(description = "권한", example = "USER")
    private Role role;

    @Schema(description = "학과", example = "COMPUTER_SCIENCE")
    private Department department;

    @Schema(description = "포털 인증 여부", example = "true")
    private Boolean isSchoolVerified;

    @Schema(description = "CloudFront 이미지 url", example = "https://d3dbvb22maaxgy.cloudfront.net/users/1/profile.png")
    private String imageUrl;

    public static MyInfoResponse create(User user, String imageUrl) {
        return new MyInfoResponse(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getStudentNumber(),
                user.getName(),
                user.getRole(),
                user.getDepartment(),
                user.getIsSchoolVerified(),
                imageUrl);
    }
}
