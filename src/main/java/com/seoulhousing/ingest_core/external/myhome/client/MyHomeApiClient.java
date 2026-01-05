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
import java.util.regex.Pattern;

@Component
public class MyHomeApiClient {

    private static final Logger log = LoggerFactory.getLogger(MyHomeApiClient.class);

    private static final Pattern SERVICE_KEY_PATTERN =
            Pattern.compile("(serviceKey=)([^&]+)",Pattern.CASE_INSENSITIVE); // 혹시 몰라서 대소문자 무시

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
        String safeUri = toSafeLogUri(uri); //안전하게 마스킹 해두기

        RestClient.RequestHeadersSpec<?> spec = myHomeRestClient.get().uri(uri);
        spec = spec.accept(MediaType.APPLICATION_JSON);

        MyHomeListResponse res = spec.retrieve().body(MyHomeListResponse.class);

        validateResponse("RSDT", safeUri, res);
        return res;
    }

    // 공공분양 전용 콜
    private MyHomeListResponse callLtRsdt(MultiValueMap<String, String> queryParams) {
        URI uri = buildUri(PATH_LTRSDT_LIST, queryParams);
        String safeUri = toSafeLogUri(uri); //안전하게 마스킹 해두기

        RestClient.RequestHeadersSpec<?> spec = myHomeRestClient.get().uri(uri);
        spec = spec.accept(MediaType.APPLICATION_JSON);

        MyHomeListResponse res = spec.retrieve().body(MyHomeListResponse.class);

        validateResponse("LTRSDT", safeUri, res);
        return res;
    }

    // 응답구조 및 성공 코드 검증
    private void validateResponse(String category, String safeUri, MyHomeListResponse res) {

        // body가 널 값인지 검사하기
        if (res == null) {
            log.error("[MyHome][{}] response body is null. uri={}", category, safeUri);
            throw new IllegalStateException("MyHome API 응답이 null 입니다.");
        }

        // 결과 코드 및 메시지
        String code = res.getResultCode();
        String msg  = res.getResultMsg();


        if (code == null) {
            log.error("[MyHome][{}] resultCode is null (header missing). uri={}", category, safeUri);
            throw new IllegalStateException("MyHome API 응답이 비정상(header/resultCode null)");
        }

        // 성공 코드가 아니면 비즈니스 실패로 처리
        if (!"00".equals(code)) {
            log.error("[MyHome][{}] api failure. uri={}, resultCode={}, resultMsg={}",
                    category, safeUri, code, msg);
            throw new IllegalStateException("MyHome API 실패: " + msg);
        }

        // 성공코드인데 body가 널이면 추적 로그남김
        MyHomeListResponse.Body body = res.getBody();
        if (body == null) {
            log.error("[MyHome][{}] body is null though resultCode=00. uri={}", category, safeUri);
            throw new IllegalStateException("MyHome API 응답 구조가 비정상입니다(body null)");
        }

        // item이 널이면 추적로그남김
        if (body.getItem() == null) {
            log.error("[MyHome][{}] item is null though resultCode=00. uri={}, totalCount={}",
                    category, safeUri, body.getTotalCount());
            throw new IllegalStateException("MyHome API 응답 구조가 비정상입니다(item null)");
        }
    }

    private static String toSafeLogUri(URI uri) {
        if (uri == null) return "null";

        String path = (uri.getRawPath() == null) ? "" : uri.getRawPath();
        String query = uri.getRawQuery();

        String raw = (query == null || query.isBlank())
                ? path
                : path + "?" + query;

        return maskServiceKey(raw);
    }

    private static String maskServiceKey(String raw) {
        if (raw == null) return "null";
        return SERVICE_KEY_PATTERN.matcher(raw).replaceAll("$1****");
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