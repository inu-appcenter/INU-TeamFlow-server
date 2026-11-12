package com.inuteamflow.server.global.logging;

import java.util.Map;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

/**
 * 비동기 작업을 제출한 스레드의 MDC(traceId, requestId, userId 등)를 작업 실행 스레드로 복사한다.
 *
 * <p>스레드 풀의 스레드는 재사용되므로 작업이 끝나면 MDC를 비운다.</p>
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> {
            if (contextMap != null) {
                MDC.setContextMap(contextMap);
            }
            try {
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}
