package com.inuteamflow.server.global;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity extends BaseTimeEntity {

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", nullable = false)
    private Long updatedBy;

    // WebSocket/STOMP 등 SecurityContextHolder가 자동으로 채워지지 않는 흐름에서
    // 수동으로 작성자를 지정하기 위한 메서드. AuditorAware가 정상 동작하면
    // 같은 값으로 다시 채워지므로 충돌 방지
    public void assignAuditor(Long userId) {
        this.createdBy = userId;
        this.updatedBy = userId;
    }

}
