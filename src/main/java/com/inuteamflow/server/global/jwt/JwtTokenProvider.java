package com.inuteamflow.server.global.jwt;

import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import com.inuteamflow.server.domain.user.service.UserDetailsServiceImpl;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import com.inuteamflow.server.global.jwt.refresh.RefreshToken;
import com.inuteamflow.server.global.jwt.refresh.RefreshTokenRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    public static final long ACCESS_TOKEN_VALID_TIME = 3 * 60 * 60 * 1000L; // 3시간
    public static final long REFRESH_TOKEN_VALID_TIME = 3 * 24 * 60 * 60 * 1000L; // 3일

    private final Key key;
    private final UserDetailsServiceImpl userDetailsService;
    private final RefreshTokenRepository refreshTokenRepository;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secretKey,
            UserDetailsServiceImpl userDetailsService,
            RefreshTokenRepository refreshTokenRepository) {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.userDetailsService = userDetailsService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public TokenResponse generateToken(Authentication authentication) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userDetails.getUser();

        String accessToken = createAccessToken(user.getUsername(), authorities);
        String refreshToken = createRefreshToken(user.getUsername());
        upsertRefreshToken(user, refreshToken);

        return TokenResponse.builder()
                .grantType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public Authentication getAuthentication(String accessToken) {
        Claims claims = parseClaims(accessToken);
        if (claims.get("auth") == null) {
            throw new RestApiException(CustomErrorCode.JWT_INVALID);
        }

        Collection<? extends GrantedAuthority> authorities = Arrays.stream(
                        claims.get("auth").toString().split(","))
                .map(SimpleGrantedAuthority::new)
                .toList();
        UserDetails userDetails = userDetailsService.loadUserByUsername(claims.getSubject());
        return new UsernamePasswordAuthenticationToken(userDetails, "", authorities);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (MalformedJwtException | SecurityException e) {
            throw new RestApiException(CustomErrorCode.JWT_MALFORMED);
        } catch (ExpiredJwtException e) {
            throw new RestApiException(CustomErrorCode.JWT_EXPIRED);
        } catch (UnsupportedJwtException e) {
            throw new RestApiException(CustomErrorCode.JWT_UNSUPPORTED);
        } catch (IllegalArgumentException e) {
            throw new RestApiException(CustomErrorCode.JWT_INVALID);
        }
    }

    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(accessToken)
                    .getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    private String createAccessToken(String username, String authorities) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(username)
                .claim("auth", authorities)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(ACCESS_TOKEN_VALID_TIME)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private String createRefreshToken(String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(REFRESH_TOKEN_VALID_TIME)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public TokenResponse generateTokenByUsername(String username) {
        UserDetailsImpl userDetails = userDetailsService.loadUserByUsername(username);
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        return generateToken(authentication);
    }

    private void upsertRefreshToken(User user, String refreshTokenValue) {
        refreshTokenRepository
                .findByUserId(user.getUserId())
                .ifPresentOrElse(
                        refreshToken -> refreshToken.updateToken(refreshTokenValue),
                        () -> refreshTokenRepository.save(RefreshToken.builder()
                                .user(user)
                                .refreshToken(refreshTokenValue)
                                .build()));
    }
}
