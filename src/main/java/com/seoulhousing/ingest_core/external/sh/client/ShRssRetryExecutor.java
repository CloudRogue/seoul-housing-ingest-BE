package com.seoulhousing.ingest_core.external.sh.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

import java.util.function.Supplier;

@Component
public class ShRssRetryExecutor {

    private static final Logger log = LoggerFactory.getLogger(ShRssRetryExecutor.class);

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
        log.error("[SH][RSS][{}] retry exhausted. ex={}", label, e.getClass().getSimpleName());

        throw new IllegalStateException("SH RSS 재시도 실패: ", e);
    }
}
