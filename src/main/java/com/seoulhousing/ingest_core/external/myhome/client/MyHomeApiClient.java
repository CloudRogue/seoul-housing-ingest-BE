package com.seoulhousing.ingest_core.external.myhome.client;

import com.seoulhousing.ingest_core.config.ExternalMyHomeProperties;
import com.seoulhousing.ingest_core.external.myhome.dto.LtRsdtListRequest;
import com.seoulhousing.ingest_core.external.myhome.dto.MyHomeListResponse;
import com.seoulhousing.ingest_core.external.myhome.dto.RsdtListRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
public class MyHomeApiClient {

    private final RestClient myHomeRestClient;
    private final ExternalMyHomeProperties properties;
    private final MyHomeRetryExecutor retry;

    private static final String PATH_RSDT_LIST = "/rsdtRcritNtcList";     // 공공임대
    private static final String PATH_LTRSDT_LIST = "/ltRsdtRcritNtcList"; // 공공분양

    public MyHomeApiClient(
            @Qualifier("myHomeRestClient") RestClient myHomeRestClient,
            ExternalMyHomeProperties properties,
            MyHomeRetryExecutor retry
    ) {
        this.myHomeRestClient = myHomeRestClient;
        this.properties = properties;
        this.retry = retry;
    }

    public MyHomeListResponse fetchRsdt(RsdtListRequest request) {
        if (request == null) throw new IllegalArgumentException("request must not be null");
        return retry.run(() -> call(PATH_RSDT_LIST, request.toQueryParams()));
    }

    public MyHomeListResponse fetchLtRsdt(LtRsdtListRequest request) {
        if (request == null) throw new IllegalArgumentException("request must not be null");
        return retry.run(() -> call(PATH_LTRSDT_LIST, request.toQueryParams()));
    }

    private MyHomeListResponse call(String path, MultiValueMap<String, String> queryParams) {
        URI uri = buildUri(path, queryParams);

        RestClient.RequestHeadersUriSpec<?> uriSpec = myHomeRestClient.get();
        RestClient.RequestHeadersSpec<?> headersSpec = uriSpec.uri(uri);
        headersSpec = headersSpec.accept(MediaType.APPLICATION_JSON);

        MyHomeListResponse res = headersSpec
                .retrieve()
                .body(MyHomeListResponse.class);

        if (res == null) throw new IllegalStateException("MyHome API 응답이 null 입니다.");
        res.requireSuccess();
        return res;
    }

    private URI buildUri(String path, MultiValueMap<String, String> queryParams) {
        UriComponentsBuilder b = UriComponentsBuilder
                .fromUriString(properties.getBaseUrl())
                .path(path)
                .queryParam("serviceKey", properties.getServiceKey())
                .queryParam("_type", "json");

        queryParams.forEach((k, values) -> values.forEach(v -> b.queryParam(k, v)));
        return b.build(true).toUri();
    }


}