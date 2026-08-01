package com.inuteamflow.server.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration
@ConditionalOnProperty(name = "oracle.enabled", havingValue = "true")
public class OracleConfig {

    @Bean(name = "oracleJdbcTemplate")
    public JdbcTemplate oracleJdbc(
            @Value("${oracle.datasource.jdbc-url}") String jdbcUrl,
            @Value("${oracle.datasource.username}") String username,
            @Value("${oracle.datasource.password}") String password,
            @Value("${oracle.datasource.driver-class-name}") String driverClassName) {
        DriverManagerDataSource oracleDataSource = new DriverManagerDataSource();
        oracleDataSource.setDriverClassName(driverClassName);
        oracleDataSource.setUrl(jdbcUrl);
        oracleDataSource.setUsername(username);
        oracleDataSource.setPassword(password);

        return new JdbcTemplate(oracleDataSource);
    }
}
