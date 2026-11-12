package com.inuteamflow.server.global.config;

import com.inuteamflow.server.global.logging.MdcTaskDecorator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
@EnableAsync
@Configuration
public class AsyncConfig {

    private static final int PUSH_CORE_POOL_SIZE = 2;
    private static final int PUSH_MAX_POOL_SIZE = 4;
    private static final int PUSH_QUEUE_CAPACITY = 500;
    private static final int PUSH_AWAIT_TERMINATION_SECONDS = 30;

    /**
     * 푸시 알림 발송 전용 스레드 풀을 생성한다.
     *
     * <p>요청 스레드가 Firebase, Expo 발송을 기다리지 않도록 발송을 이 스레드 풀에서 실행한다.
     * 대기열이 가득 차면 요청 처리에 영향을 주지 않도록 작업을 버리고 기록한다.
     * 서버 종료 시 진행 중인 발송은 최대 {@value #PUSH_AWAIT_TERMINATION_SECONDS}초까지 기다린다.</p>
     *
     * @return 푸시 발송용 스레드 풀
     */
    @Bean
    public ThreadPoolTaskExecutor pushTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(PUSH_CORE_POOL_SIZE);
        executor.setMaxPoolSize(PUSH_MAX_POOL_SIZE);
        executor.setQueueCapacity(PUSH_QUEUE_CAPACITY);
        executor.setThreadNamePrefix("push-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setRejectedExecutionHandler((task, pool) -> log.warn(
                "푸시 발송 대기열이 가득 차 발송을 건너뜀 - queueSize: {}", pool.getQueue().size()));
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(PUSH_AWAIT_TERMINATION_SECONDS);
        return executor;
    }
}
