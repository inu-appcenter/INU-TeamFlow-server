package com.inuteamflow.server.global;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

// BaseTimeEntity가 이미 @EntityListeners(AuditingEntityListener.class)를 선언하고 있고 이 클래스가 그걸
// 상속하므로 여기서 다시 선언하지 않는다. (중복 선언 시 JPA가 리스너를 두 번 등록해 auditing 콜백이
// 두 번 실행됨 - assignAuditor()로 지정한 값이 두 번째 호출에서 덮어써지는 버그의 원인이었음)
@Getter
@MappedSuperclass
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
    }

    public static Long consumeManualAuditor() {
        Long value = MANUAL_AUDITOR.get();
        MANUAL_AUDITOR.remove();
        return value;
    }
}
