package com.inuteamflow.server.global.config;

import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<Long> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.empty();
            }

            if (!(authentication.getPrincipal() instanceof UserDetailsImpl userDetails)) {
                return Optional.empty();
            }

            return Optional.ofNullable(userDetails.getUser().getUserId());
        };
    }
}
