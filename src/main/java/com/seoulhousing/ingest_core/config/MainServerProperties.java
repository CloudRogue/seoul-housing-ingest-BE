package com.seoulhousing.ingest_core.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Validated
@ConfigurationProperties(prefix = "main-server")
public class MainServerProperties {

    // 메인서버 base url
    @NotBlank
    private final String baseUrl;

    //메인서버 ingest endpoint
    @NotBlank
    private final String ingestPath;

    public MainServerProperties(String baseUrl, String ingestPath) {
        this.baseUrl = baseUrl;
        this.ingestPath = ingestPath;
    }
}
