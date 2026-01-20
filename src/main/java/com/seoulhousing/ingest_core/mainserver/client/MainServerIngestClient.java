package com.seoulhousing.ingest_core.mainserver.client;

import com.seoulhousing.ingest_core.config.MainServerProperties;
import com.seoulhousing.ingest_core.mainserver.dto.AnnouncementIngestRequest;
import com.seoulhousing.ingest_core.mainserver.dto.IngestResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class MainServerIngestClient {

    private static final Logger log = LoggerFactory.getLogger(MainServerIngestClient.class); // 로거

    private final RestClient mainServerRestClient;
    private final MainServerProperties properties;
    private final MainServerRetryExecutor retry;


    public MainServerIngestClient(
            @Qualifier("mainServerRestClient") RestClient mainServerRestClient,
            MainServerProperties properties,
            MainServerRetryExecutor retry
    ) {
        this.mainServerRestClient = mainServerRestClient;
        this.properties = properties;
        this.retry = retry;
    }

    // 외부가 호출하는 메서드
    public IngestResponse ingest(AnnouncementIngestRequest request) {

        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }

        return retry.run("INGEST", () -> doIngest(request));
    }

    // 실제 http 호출 수행
    private IngestResponse doIngest(AnnouncementIngestRequest request) {

        // 혹시몰라 경로 정규화
        String path = normalizePath(properties.getIngestPath());

        try {
            // 요청수행 및  응답 받기
            IngestResponse res = mainServerRestClient
                    .post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(IngestResponse.class);


            if (res == null) {
                throw new IllegalStateException("MainServer ingest response is null");
            }


            log.info("[MainServer] ingest ok. received={}, created={}, updated={}, skipped={}",
                    res.received(), res.created(), res.updated(), res.skipped());


            return res;

        } catch (RestClientResponseException e) {

            int status = e.getStatusCode().value();
            String body = safeBody(e.getResponseBodyAsString());  // 응답 바디

            // 실패 로그
            log.error("[MainServer] ingest http fail. status={}, body={}", status, body);

            throw new IllegalStateException("MainServer ingest http fail: " + status, e);

        } catch (Exception e) {
            // 예상치 못한 예외
            log.error("[MainServer] ingest unexpected fail. ex={}", e.getClass().getSimpleName(), e);
            throw e;
        }
    }

    // 경로 정규화
    private static String normalizePath(String raw) {

        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("main-server.ingest-path is blank");
        }

        // trim 후 앞에 / 없으면 붙여줌
        String p = raw.trim();
        return p.startsWith("/") ? p : "/" + p;
    }

    // 로그가 너무 길어지는 걸 방지하기 위한 바디 컷
    private static String safeBody(String body) {

        if (body == null) return "null";
        // 최대 2000자 정도
        if (body.length() > 2000) {
            return body.substring(0, 2000) + "...";
        }
        return body;
    }
}
