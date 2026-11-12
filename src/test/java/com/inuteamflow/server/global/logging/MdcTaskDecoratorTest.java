package com.inuteamflow.server.global.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

/**
 * {@link MdcTaskDecorator}가 작업 제출 스레드의 MDC를 실행 스레드로 전달하고, 실행 후 비우는지 검증한다.
 */
class MdcTaskDecoratorTest {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MdcTaskDecorator decorator = new MdcTaskDecorator();

    @AfterEach
    void tearDown() {
        MDC.clear();
        executor.shutdownNow();
    }

    @Test
    @DisplayName("작업 제출 시점의 MDC를 실행 스레드에서 사용할 수 있다")
    void decorate_copiesMdcToWorkerThread() throws Exception {
        MDC.put("traceId", "trace-1");
        MDC.put("userId", "7");
        AtomicReference<Map<String, String>> captured = new AtomicReference<>();

        executor.submit(decorator.decorate(() -> captured.set(MDC.getCopyOfContextMap())))
                .get(1, TimeUnit.SECONDS);

        assertThat(captured.get()).containsEntry("traceId", "trace-1").containsEntry("userId", "7");
    }

    @Test
    @DisplayName("작업이 끝나면 실행 스레드의 MDC를 비워 다음 작업에 섞이지 않는다")
    void decorate_clearsMdcAfterRun() throws Exception {
        MDC.put("traceId", "trace-1");
        executor.submit(decorator.decorate(() -> {})).get(1, TimeUnit.SECONDS);

        MDC.clear();
        AtomicReference<Map<String, String>> captured = new AtomicReference<>();
        executor.submit(() -> captured.set(MDC.getCopyOfContextMap())).get(1, TimeUnit.SECONDS);

        assertThat(captured.get()).isNullOrEmpty();
    }
}
