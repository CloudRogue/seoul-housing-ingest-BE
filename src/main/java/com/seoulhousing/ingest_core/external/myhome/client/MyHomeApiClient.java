package com.seoulhousing.ingest_core.external.myhome.client;

import com.seoulhousing.ingest_core.config.ExternalMyHomeProperties;
import com.seoulhousing.ingest_core.external.myhome.dto.LtRsdtListRequest;
import com.seoulhousing.ingest_core.external.myhome.dto.MyHomeListResponse;
import com.seoulhousing.ingest_core.external.myhome.dto.RsdtListRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
public class MyHomeApiClient {

    private static final Logger log = LoggerFactory.getLogger(MyHomeApiClient.class);

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

    // 공공임대
    public MyHomeListResponse fetchRsdt(RsdtListRequest request) {
        if (request == null) throw new IllegalArgumentException("request must not be null");
        return retry.run("RSDT", () -> callRsdt(request.toQueryParams()));
    }

    //공공분양
    public MyHomeListResponse fetchLtRsdt(LtRsdtListRequest request) {
        if (request == null) throw new IllegalArgumentException("request must not be null");
        return retry.run("LTRSDT", () -> callLtRsdt(request.toQueryParams()));
    }

    // 공공임대 전용 콜
    private MyHomeListResponse callRsdt(MultiValueMap<String, String> queryParams) {
        URI uri = buildUri(PATH_RSDT_LIST, queryParams);

        RestClient.RequestHeadersSpec<?> spec = myHomeRestClient.get().uri(uri);
        spec.accept(MediaType.APPLICATION_JSON);

        MyHomeListResponse res = spec.retrieve().body(MyHomeListResponse.class);

        validateResponse("RSDT", uri, res);
        return res;
    }

    // 공공분양 전용 콜
    private MyHomeListResponse callLtRsdt(MultiValueMap<String, String> queryParams) {
        URI uri = buildUri(PATH_LTRSDT_LIST, queryParams);

        RestClient.RequestHeadersSpec<?> spec = myHomeRestClient.get().uri(uri);
        spec.accept(MediaType.APPLICATION_JSON);

        MyHomeListResponse res = spec.retrieve().body(MyHomeListResponse.class);

        validateResponse("LTRSDT", uri, res);
        return res;
    }

    // 응답구조 및 성공 코드 검증
    private void validateResponse(String category, URI uri, MyHomeListResponse res) {

        // body가 널 값인지 검사하기
        if (res == null) {
            log.error("[MyHome][{}] response body is null. uri={}", category, uri);
            throw new IllegalStateException("MyHome API 응답이 null 입니다.");
        }

        // 결과 코드 및 메시지
        String code = res.getResultCode();
        String msg  = res.getResultMsg();


        if (code == null) {
            log.error("[MyHome][{}] resultCode is null (header missing). uri={}", category, uri);
            throw new IllegalStateException("MyHome API 응답이 비정상(header/resultCode null)");
        }

        // 성공 코드가 아니면 비즈니스 실패로 처리
        if (!"00".equals(code)) {
            log.warn("[MyHome][{}] api failure. uri={}, resultCode={}, resultMsg={}",
                    category, uri, code, msg);
            throw new IllegalStateException("MyHome API 실패: " + msg);
        }

        // 성공코드인데 body가 널이면 추적 로그남김
        MyHomeListResponse.Body body = res.getBody();
        if (body == null) {
            log.warn("[MyHome][{}] body is null though resultCode=00. uri={}", category, uri);
            return;
        }

        // item이 널이면 추적로그남김
        if (body.getItem() == null) {
            log.warn("[MyHome][{}] item is null though resultCode=00. uri={}, totalCount={}",
                    category, uri, body.getTotalCount());
        }
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