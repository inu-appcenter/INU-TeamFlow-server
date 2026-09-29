package com.inuteamflow.server.domain.user.service;

import com.inuteamflow.server.domain.user.dto.request.LoginRequest;
import com.inuteamflow.server.domain.user.dto.request.SignupRequest;
import com.inuteamflow.server.domain.user.dto.request.VerifySchoolRequest;
import com.inuteamflow.server.domain.user.dto.response.MyInfoResponse;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.enums.Role;
import com.inuteamflow.server.domain.user.repository.SchoolLoginRepository;
import com.inuteamflow.server.domain.user.repository.UserRepository;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import com.inuteamflow.server.global.jwt.JwtTokenProvider;
import com.inuteamflow.server.global.jwt.TokenResponse;
import com.inuteamflow.server.global.jwt.refresh.RefreshToken;
import com.inuteamflow.server.global.jwt.refresh.RefreshTokenRepository;
import com.inuteamflow.server.global.s3.S3Service;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final S3Service s3Service;
    private final UserRepository userRepository;
    private final ObjectProvider<SchoolLoginRepository> schoolLoginRepositoryProvider;
    private final JwtTokenProvider jwtTokenProvider;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * 새로운 사용자를 가입시킨다.
     *
     * @param request 가입할 사용자 정보
     * @return 가입된 사용자 정보
     * @throws RestApiException 사용자 이름 또는 이메일이 이미 사용 중인 경우
     */
    @Transactional
    public MyInfoResponse signUp(SignupRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RestApiException(CustomErrorCode.USER_USERNAME_CONFLICT);
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RestApiException(CustomErrorCode.USER_EMAIL_CONFLICT);
        }

        User user = User.create(request, bCryptPasswordEncoder.encode(request.getPassword()));
        String imageUrl = s3Service.getImageUrl(user.getImageKey());

        return MyInfoResponse.of(userRepository.save(user), imageUrl);
    }

    /**
     * 사용자 자격 증명을 검증하고 토큰을 발급한다.
     *
     * <p>정지/영구정지된 사용자는 {@link UserDetailsImpl#isAccountNonLocked()}에 의해 동일하게
     * {@link LockedException}으로 처리되므로, 실제 상태를 다시 조회해 임시 정지와 영구정지를 구분한다.</p>
     *
     * @param request 로그인 자격 증명
     * @return 발급된 액세스 토큰과 리프레시 토큰
     * @throws RestApiException 임시 정지 또는 영구정지된 사용자인 경우
     */
    @Transactional
    public TokenResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManagerBuilder
                    .getObject()
                    .authenticate(
                            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (LockedException e) {
            throw resolveLockedUserException(request.getUsername());
        }
        return jwtTokenProvider.generateToken(authentication);
    }

    /**
     * 계정 잠김으로 로그인이 거부된 사용자의 실제 제재 상태(임시 정지/영구정지)를 조회해 알맞은 예외를 반환한다.
     *
     * @param username 로그인을 시도한 사용자의 아이디
     * @return 사용자 상태에 맞는 {@link RestApiException}
     */
    private RestApiException resolveLockedUserException(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user != null && user.getRole() == Role.BANNED) {
            return new RestApiException(CustomErrorCode.USER_BANNED);
        }
        if (user != null && user.getSuspendedUntil() != null) {
            long remainingDays = Duration.between(LocalDateTime.now(ZoneId.systemDefault()), user.getSuspendedUntil())
                            .toDays()
                    + 1;
            String until = user.getSuspendedUntil().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            return new RestApiException(
                    CustomErrorCode.USER_SUSPENDED,
                    "임시 정지된 사용자입니다. (해제일: " + until + ", 남은 기간: " + remainingDays + "일)");
        }
        return new RestApiException(CustomErrorCode.USER_SUSPENDED);
    }

    /**
     * 리프레시 토큰으로 토큰을 재발급한다.
     *
     * @param refreshToken 재발급에 사용할 리프레시 토큰
     * @return 재발급된 액세스 토큰과 리프레시 토큰
     * @throws RestApiException 리프레시 토큰을 찾을 수 없거나 저장된 토큰과 일치하지 않는 경우,
     *                          또는 사용자를 찾을 수 없는 경우
     */
    @Transactional
    public TokenResponse reissue(String refreshToken) {
        RefreshToken savedRefreshToken = refreshTokenRepository
                .findByRefreshToken(refreshToken)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.JWT_REFRESH_NOT_FOUND));

        if (!savedRefreshToken.getRefreshToken().equals(refreshToken)) {
            throw new RestApiException(CustomErrorCode.JWT_REFRESH_NOT_MATCH);
        }

        User user = userRepository
                .findById(savedRefreshToken.getUserId())
                .orElseThrow(() -> new RestApiException(CustomErrorCode.USER_NOT_FOUND));
        return jwtTokenProvider.generateTokenByUsername(user.getUsername());
    }

    /**
     * 로그인한 사용자의 재학 여부를 인증한다.
     *
     * <p>학교 포털 인증에 성공하고 학번이 중복되지 않은 경우 사용자에게 학번을 등록한다.</p>
     *
     * @param user 재학 여부를 인증할 사용자
     * @param request 학교 포털 인증 정보
     * @return 재학 인증이 반영된 사용자 정보
     * @throws RestApiException 이미 재학 인증을 완료했거나 학교 인증 기능을 사용할 수 없는 경우,
     *                          또는 포털 인증에 실패하거나 학번이 이미 사용 중인 경우,
     *                          또는 사용자를 찾을 수 없는 경우
     */
    @Transactional
    public MyInfoResponse verifySchool(User user, VerifySchoolRequest request) {
        if (user.getIsSchoolVerified()) {
            throw new RestApiException(CustomErrorCode.USER_SCHOOL_ALREADY_VERIFIED);
        }

        SchoolLoginRepository schoolLoginRepository = schoolLoginRepositoryProvider.getIfAvailable();
        if (schoolLoginRepository == null) {
            throw new RestApiException(CustomErrorCode.USER_SCHOOL_VERIFY_UNAVAILABLE);
        }

        String loginCheckResult =
                schoolLoginRepository.loginCheck(request.getStudentNumber(), request.getPortalPassword());

        if (loginCheckResult == null || "N".equalsIgnoreCase(loginCheckResult.trim())) {
            throw new RestApiException(CustomErrorCode.USER_SCHOOL_VERIFY_FAILED);
        }

        if (userRepository.existsByStudentNumber(request.getStudentNumber())) {
            throw new RestApiException(CustomErrorCode.USER_STUDENT_NUMBER_CONFLICT);
        }

        User requester = userRepository
                .findById(user.getUserId())
                .orElseThrow(() -> new RestApiException(CustomErrorCode.USER_NOT_FOUND));
        requester.verifySchool(request.getStudentNumber());

        String imageUrl = s3Service.getImageUrl(requester.getImageKey());
        return MyInfoResponse.of(requester, imageUrl);
    }
}
