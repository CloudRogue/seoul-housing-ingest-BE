package com.seoulhousing.ingest_core.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Validated
@ConfigurationProperties(prefix = "external.sh.rss")
public class ExternalShRssProperties {

    @NotBlank
    private final String noticeUrl;

    @Min(100)
    private final long connectTimeoutMs;

    @Min(100)
    private final long readTimeoutMs;

    public ExternalShRssProperties(String noticeUrl, long connectTimeoutMs, long readTimeoutMs) {
        this.noticeUrl = noticeUrl;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }
}
