package com.inuteamflow.server.domain.intip.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "intip_sync_cursor")
@NoArgsConstructor
public class IntipSyncCursor {

    public static final Long SINGLETON_ID = 1L; // 항상 한 행만 존재하는 싱글턴 커서

    @Id
    private Long id;

    @Column(name = "last_processed_notice_id")
    private Long lastProcessedNoticeId; // 아직 한 번도 동기화 안 했으면 null

    public void updateCursor(Long lastProcessedNoticeId) {
        this.lastProcessedNoticeId = lastProcessedNoticeId;
    }
}
