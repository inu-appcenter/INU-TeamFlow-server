package com.inuteamflow.server.domain.user.entity;

import com.inuteamflow.server.domain.user.dto.request.SignupRequest;
import com.inuteamflow.server.domain.user.dto.request.UserUpdateRequest;
import com.inuteamflow.server.domain.user.enums.Department;
import com.inuteamflow.server.domain.user.enums.Role;
import com.inuteamflow.server.global.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "department", nullable = false)
    @Enumerated(EnumType.STRING)
    private Department department;

    @Column(name = "student_number", unique = true)
    private String studentNumber;

    @Column(name = "is_school_verified")
    private Boolean isSchoolVerified;

    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "image_key")
    private String imageKey;

    @Builder
    private User(
            String username,
            String email,
            String password,
            String name,
            Department department,
            String studentNumber,
            Boolean isSchoolVerified,
            Role role,
            String imageKey) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.name = name;
        this.department = department;
        this.studentNumber = studentNumber;
        this.isSchoolVerified = isSchoolVerified;
        this.role = role;
        this.imageKey = imageKey;
    }

    public static User create(SignupRequest request, String encode) {
        return User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(encode)
                .name(request.getName())
                .department(request.getDepartment())
                .studentNumber(null)
                .isSchoolVerified(false)
                .role(Role.USER)
                .imageKey(request.getImageKey())
                .build();
    }

    public void update(UserUpdateRequest request, String encodedPassword) {
        if (encodedPassword != null) {
            this.password = encodedPassword;
        }
        this.email = request.getEmail();
        this.name = request.getName();
        this.department = request.getDepartment();
        this.imageKey = request.getImageKey();
    }

    public void verifySchool(String studentNumber) {
        this.studentNumber = studentNumber;
        this.isSchoolVerified = true;
    }
}
