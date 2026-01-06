package com.seoulhousing.ingest_core.redis.config;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Validated
@ConfigurationProperties(prefix = "ingest.snapshot")
public class IngestSnapShotProperties {

    @Min(1)
    private final int ttlDays;

    @Min(1)
    private final int gzipThresholdBytes;

    public IngestSnapShotProperties(int ttlDays, int gzipThresholdBytes) {
        this.ttlDays = ttlDays;
        this.gzipThresholdBytes = gzipThresholdBytes;
    }
}
