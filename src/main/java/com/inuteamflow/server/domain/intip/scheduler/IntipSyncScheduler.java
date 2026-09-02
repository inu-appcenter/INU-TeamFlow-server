package com.inuteamflow.server.domain.intip.scheduler;

import com.inuteamflow.server.domain.intip.service.IntipSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IntipSyncScheduler {

    private final IntipSyncService intipSyncService;

    @Scheduled(cron = "0 */15 * * * *") // 15분마다, 서버 타임존 기준
//    @Scheduled(cron = "0/10 * * * * *") // 테스트용: 10초마다
    public void run() {
        intipSyncService.sync();
    }
}
