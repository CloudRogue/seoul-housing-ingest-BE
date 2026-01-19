package com.seoulhousing.ingest_core.mainserver.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

import java.util.function.Supplier;

@Component
public class MainServerRetryExecutor {

    private static final Logger log = LoggerFactory.getLogger(MainServerRetryExecutor.class);

    @Retryable(
            // 네트워크/타임아웃류만 여기서  재시도
            retryFor = ResourceAccessException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 300, multiplier = 2.0, maxDelay = 2000)
    )
    public <T> T run(String label, Supplier<T> supplier) {
        return supplier.get();
    }

    @Recover
    public <T> T recover(ResourceAccessException e, String label, Supplier<T> supplier) {
        log.error("[MainServer][{}] retry exhausted. ex={}", label, e.getClass().getSimpleName());
        throw new IllegalStateException("MainServer ingest 재시도 실패", e);
    }
}
