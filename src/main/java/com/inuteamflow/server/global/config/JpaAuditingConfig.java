package com.inuteamflow.server.global.config;

import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import com.inuteamflow.server.global.BaseEntity;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<Long> auditorProvider() {
        return () -> {
            Long manualAuditor = BaseEntity.consumeManualAuditor();
            if (manualAuditor != null) {
                log.info(
                        "[AUDIT 디버그] manualAuditor 사용 thread={} value={}",
                        Thread.currentThread().getName(),
                        manualAuditor); // 임시 디버그용, 확인 후 제거
                return Optional.of(manualAuditor);
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            log.info(
                    "[AUDIT 디버그] manualAuditor 없음, SecurityContext 확인 thread={} authentication={}",
                    Thread.currentThread().getName(),
                    authentication); // 임시 디버그용, 확인 후 제거
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
