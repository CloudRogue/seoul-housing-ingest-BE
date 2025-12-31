package com.seoulhousing.ingest_core.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import java.net.http.HttpClient;
import java.time.Duration;

/**
 * 외부 공공데이터(MyHome) API 호출에 사용할 RestClient Bean 설정.
 *
 * <p>application.yml에 정의된 external.myhome 설정값을 {@link ExternalMyHomeProperties}로 바인딩 받아
 * baseUrl 및 timeout(connect/read)을 공통 적용한 RestClient를 생성한다.</p>
 */

@Configuration
@EnableConfigurationProperties(ExternalMyHomeProperties.class)
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
}
