package com.seoulhousing.ingest_core.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import java.net.http.HttpClient;
import java.time.Duration;



@Configuration
@EnableConfigurationProperties({ExternalMyHomeProperties.class, ExternalShRssProperties.class})
public class RestClientConfig {

    @Bean("myHomeRestClient")
    public RestClient myHomeRestClient(ExternalMyHomeProperties properties) {
        // JDK 기본 HttpClient를 생성하고 타임 아웃을 설정한다
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .build();

        // 스프링 RestClient가 사용할 요청 팩토리를 만든다
        // 응답 타임아웃 설정가능하다
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));

        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Bean("shRssRestClient")
    public RestClient shRssRestClient(ExternalShRssProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));

        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }
}
