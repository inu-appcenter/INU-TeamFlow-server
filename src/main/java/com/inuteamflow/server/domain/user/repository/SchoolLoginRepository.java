package com.inuteamflow.server.domain.user.repository;

import com.inuteamflow.server.global.logging.LogMasker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "oracle.enabled", havingValue = "true")
public class SchoolLoginRepository {

    private static final String LOGIN_CHECK_SQL = "SELECT F_LOGIN_CHECK(?, ?) FROM DUAL";

    @Qualifier("oracleJdbcTemplate")
    private final JdbcTemplate oracleJdbcTemplate;

    public String loginCheck(String username, String password) {
        log.info("학교 로그인 조회 id: {}", LogMasker.maskId(username));

        try {
            String result = oracleJdbcTemplate.queryForObject(LOGIN_CHECK_SQL, String.class, username, password);
            log.info("학교 DB 조회 완료 - 인증 성공 여부: {}", result != null && !"N".equalsIgnoreCase(result.trim()));
            return result;
        } catch (DataAccessException exception) {
            log.warn("학교 DB 연결 실패: {}", exception.getMessage());
            return null;
        }
    }
}
