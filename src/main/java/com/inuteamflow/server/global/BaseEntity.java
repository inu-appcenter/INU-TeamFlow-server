package com.inuteamflow.server.global;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Slf4j
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity extends BaseTimeEntity {

    // assignAuditor()로 지정한 값을 JPA Auditing이 덮어쓰지 않도록 잠깐 전달하는 스레드 로컬.
    private static final ThreadLocal<Long> MANUAL_AUDITOR = new ThreadLocal<>();

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", nullable = false)
    private Long updatedBy;

    // WebSocket/STOMP 등 SecurityContextHolder가 자동으로 채워지지 않는 흐름에서 수동으로 작성자를 지정하기 위한 메서드.
    public void assignAuditor(Long userId) {
        this.createdBy = userId;
        this.updatedBy = userId;
        MANUAL_AUDITOR.set(userId);
        log.info(
                "[AUDIT 디버그] assignAuditor 세팅 thread={} userId={} entity={}",
                Thread.currentThread().getName(),
                userId,
                this.getClass().getSimpleName()); // 임시 디버그용, 확인 후 제거
    }

    public static Long consumeManualAuditor() {
        Long value = MANUAL_AUDITOR.get();
        MANUAL_AUDITOR.remove();
        log.info(
                "[AUDIT 디버그] consumeManualAuditor 소비 thread={} value={}",
                Thread.currentThread().getName(),
                value); // 임시 디버그용, 확인 후 제거
        return value;
    }
}
