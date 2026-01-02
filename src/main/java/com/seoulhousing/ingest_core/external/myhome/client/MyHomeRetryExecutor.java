package com.seoulhousing.ingest_core.external.myhome.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

import java.util.function.Supplier;

@Component
public class MyHomeRetryExecutor {
    private static final Logger log = LoggerFactory.getLogger(MyHomeRetryExecutor.class); // 로깅


    @Retryable(
            retryFor = ResourceAccessException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 300, multiplier = 2.0, maxDelay = 2000)
    )
    public <T> T run(String label, Supplier<T> supplier) {
        return supplier.get();
    }

    @Recover
    public <T> T recover(ResourceAccessException e, String label, Supplier<T> supplier) {

        // 최종 실패 로그
        log.error("[MyHome][{}] retry exhausted. msg={}", label, e.getMessage(), e);

        throw new IllegalStateException("MyHome API 재시도 실패: " + e.getMessage(), e);
    }
}
