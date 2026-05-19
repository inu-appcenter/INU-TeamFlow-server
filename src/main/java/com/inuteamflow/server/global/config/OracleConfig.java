package com.inuteamflow.server.global.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(name = "oracle.enabled", havingValue = "true")
public class OracleConfig {

    // oracle.datasource 아래 값들이 HikariDataSource에 자동으로 바인딩
    @Bean(name = "oracleDataSource")
    @ConfigurationProperties(prefix = "oracle.datasource")
    public DataSource oracleDataSource(
    ) {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean(name = "oracleJdbcTemplate")
    public JdbcTemplate oracleJdbc(
            @Qualifier("oracleDataSource") DataSource oracleDataSource
    ) {
        return new JdbcTemplate(oracleDataSource);
    }
}
