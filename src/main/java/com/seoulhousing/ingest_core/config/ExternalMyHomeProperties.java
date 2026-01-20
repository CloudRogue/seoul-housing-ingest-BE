package com.seoulhousing.ingest_core.config;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 외부 공공데이터(MyHome) API 호출에 필요한 설정을 application.yml에서 바인딩 받는 클래스.
 */
@Getter
@Validated
@ConfigurationProperties(prefix = "external.myhome")
public class ExternalMyHomeProperties {

    @NotBlank
    private final String baseUrl;

    @NotBlank
    private final String serviceKey;

    @Min(100)
    private final long connectTimeoutMs;

    @Min(100)
    private final long readTimeoutMs;

    public ExternalMyHomeProperties(String baseUrl, String serviceKey, long connectTimeoutMs, long readTimeoutMs) {
        this.baseUrl = baseUrl;
        this.serviceKey = serviceKey;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

}
