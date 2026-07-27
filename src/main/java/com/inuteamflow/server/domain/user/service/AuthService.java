package com.inuteamflow.server.domain.user.service;

import com.inuteamflow.server.domain.user.dto.request.LoginRequest;
import com.inuteamflow.server.domain.user.dto.request.SignupRequest;
import com.inuteamflow.server.domain.user.dto.request.VerifySchoolRequest;
import com.inuteamflow.server.domain.user.dto.response.MyInfoResponse;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.repository.SchoolLoginRepository;
import com.inuteamflow.server.domain.user.repository.UserRepository;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import com.inuteamflow.server.global.jwt.JwtTokenProvider;
import com.inuteamflow.server.global.jwt.TokenResponse;
import com.inuteamflow.server.global.jwt.refresh.RefreshToken;
import com.inuteamflow.server.global.jwt.refresh.RefreshTokenRepository;
import com.inuteamflow.server.global.s3.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
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
    public MyInfoResponse signUp(
            SignupRequest request
    ) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RestApiException(CustomErrorCode.USER_USERNAME_CONFLICT);
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RestApiException(CustomErrorCode.USER_EMAIL_CONFLICT);
        }

        User user = User.create(request, bCryptPasswordEncoder.encode(request.getPassword()));
        String imageUrl = s3Service.getImageUrl(user.getImageKey());

        return MyInfoResponse.create(userRepository.save(user), imageUrl);
    }

    /**
     * 사용자 자격 증명을 검증하고 토큰을 발급한다.
     *
     * @param request 로그인 자격 증명
     * @return 발급된 액세스 토큰과 리프레시 토큰
     */
    @Transactional
    public TokenResponse login(
            LoginRequest request
    ) {
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        return jwtTokenProvider.generateToken(authentication);
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
    public TokenResponse reissue(
            String refreshToken
    ) {
        RefreshToken savedRefreshToken = refreshTokenRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.JWT_REFRESH_NOT_FOUND));

        if (!savedRefreshToken.getRefreshToken().equals(refreshToken)) {
            throw new RestApiException(CustomErrorCode.JWT_REFRESH_NOT_MATCH);
        }

        User user = userRepository.findById(savedRefreshToken.getUserId())
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
    public MyInfoResponse verifySchool(
            User user,
            VerifySchoolRequest request
    ) {
        if (user.getIsSchoolVerified()) {
            throw new RestApiException(CustomErrorCode.USER_SCHOOL_ALREADY_VERIFIED);
        }

        SchoolLoginRepository schoolLoginRepository = schoolLoginRepositoryProvider.getIfAvailable();
        if (schoolLoginRepository == null) {
            throw new RestApiException(CustomErrorCode.USER_SCHOOL_VERIFY_UNAVAILABLE);
        }

        String loginCheckResult = schoolLoginRepository.loginCheck(
                request.getStudentNumber(),
                request.getPortalPassword()
        );

        if (loginCheckResult == null || "N".equalsIgnoreCase(loginCheckResult.trim())) {
            throw new RestApiException(CustomErrorCode.USER_SCHOOL_VERIFY_FAILED);
        }

        if (userRepository.existsByStudentNumber(request.getStudentNumber())) {
            throw new RestApiException(CustomErrorCode.USER_STUDENT_NUMBER_CONFLICT);
        }

        User requester = userRepository.findById(user.getUserId())
                .orElseThrow(() -> new RestApiException(CustomErrorCode.USER_NOT_FOUND));
        requester.verifySchool(request.getStudentNumber());

        String imageUrl = s3Service.getImageUrl(requester.getImageKey());
        return MyInfoResponse.create(requester, imageUrl);
    }
}
