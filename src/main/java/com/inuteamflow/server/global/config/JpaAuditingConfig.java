package com.inuteamflow.server.global.config;

import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import com.inuteamflow.server.global.BaseEntity;
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
            Long manualAuditor = BaseEntity.consumeManualAuditor();
            if (manualAuditor != null) {
                return Optional.of(manualAuditor);
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.of(-1L); // 인증 컨텍스트 없는 배치/스케줄러는 시스템 사용자로 처리
            }

            if (!(authentication.getPrincipal() instanceof UserDetailsImpl userDetails)) {
                return Optional.of(-1L);
            }

            return Optional.ofNullable(userDetails.getUser().getUserId());
        };
    }
}
