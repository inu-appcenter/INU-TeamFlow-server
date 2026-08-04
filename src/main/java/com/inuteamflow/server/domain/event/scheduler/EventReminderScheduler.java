package com.inuteamflow.server.domain.event.scheduler;

import com.inuteamflow.server.domain.event.service.EventReminderService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventReminderScheduler {

    private final EventReminderService eventReminderService;

    @Scheduled(cron = "0 * * * * *") // 매 분 0초, 서버 타임존 기준
    public void run() {
        eventReminderService.sendDueReminders(LocalDateTime.now(ZoneId.systemDefault()));
    }
}
