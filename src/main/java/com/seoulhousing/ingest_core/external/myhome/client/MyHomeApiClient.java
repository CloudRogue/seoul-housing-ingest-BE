package com.seoulhousing.ingest_core.external.myhome.client;

import com.seoulhousing.ingest_core.config.ExternalMyHomeProperties;
import com.seoulhousing.ingest_core.external.myhome.dto.MyHomeListRequest;
import com.seoulhousing.ingest_core.external.myhome.dto.MyHomeListResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
public class MyHomeApiClient {

    private final RestClient myHomeRestClient;
    private final ExternalMyHomeProperties properties;

    public enum Category { RSDT, LTRSDT }

    private static final String PATH_RSDT_LIST = "/rsdtRcritNtcList"; // 공공 임대주택 조회 서비스
    private static final String PATH_LTRSDT_LIST = "/ltRsdtRcritNtcList"; // 공공 분약주택 조회 서비스

    public MyHomeApiClient(
            @Qualifier("myHomeRestClient") RestClient myHomeRestClient,
            ExternalMyHomeProperties properties
    ) {
        this.myHomeRestClient = myHomeRestClient;
        this.properties = properties;
    }

    public MyHomeListResponse fetchList(Category category, MyHomeListRequest request) {
        if (category == null) throw new IllegalArgumentException("category must not be null");
        if (request == null) throw new IllegalArgumentException("request must not be null");

        String path = (category == Category.RSDT) ? PATH_RSDT_LIST : PATH_LTRSDT_LIST;
        URI uri = buildUri(path, category, request);

        RestClient.RequestHeadersSpec<?> spec = myHomeRestClient.get().uri(uri);
        spec.accept(MediaType.APPLICATION_JSON);

        MyHomeListResponse res = spec.retrieve().body(MyHomeListResponse.class);

        validateResponse(res);
        return res;
    }

    private URI buildUri(String path, Category category, MyHomeListRequest request) {
        UriComponentsBuilder b = UriComponentsBuilder
                .fromUriString(properties.getBaseUrl())
                .path(path)
                .queryParam("serviceKey", properties.getServiceKey())
                .queryParam("_type", "json");

        request.toQueryParams(category).forEach((k, values) -> values.forEach(v -> b.queryParam(k, v)));
        return b.build(true).toUri();
    }

    private void validateResponse(MyHomeListResponse res) {
        if (res == null || res.getResponse() == null || res.getResponse().getHeader() == null) {
            throw new IllegalStateException("MyHome API 응답이 비정상(res/header null)");
        }
        if (!"00".equals(res.getResponse().getHeader().getResultCode())) {
            throw new IllegalStateException("MyHome API 실패: " + res.getResponse().getHeader().getResultMsg());
        }
    }
}