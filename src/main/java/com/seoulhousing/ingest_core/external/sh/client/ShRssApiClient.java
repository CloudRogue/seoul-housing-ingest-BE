package com.seoulhousing.ingest_core.external.sh.client;

import com.seoulhousing.ingest_core.config.ExternalShRssProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ShRssApiClient {

    private static final Logger log = LoggerFactory.getLogger(ShRssApiClient.class);

    private final RestClient shRssRestClient;
    private final ExternalShRssProperties properties;
    private final ShRssRetryExecutor retry;

    public ShRssApiClient(
            @Qualifier("shRssRestClient") RestClient shRssRestClient,
            ExternalShRssProperties properties,
            ShRssRetryExecutor retry
    ) {
        this.shRssRestClient = shRssRestClient;
        this.properties = properties;
        this.retry = retry;
    }

    // SH 공고/공지 RSS 원문 조회 이게 EUC-KR 인코딩 이슈가 있기에 byte[]로 받음
    public byte[] fetchNoticeRssBytes() {
        return retry.run("NOTICE",this::callNoticeRss);
    }

    //HTTP 호출을 담당하는 메서드
    private byte[] callNoticeRss(){

        String url = properties.getNoticeUrl();

        // 혹시 모르는 설정에러가 있을수도 있기에
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("external.sh.rss.notice-url is blank");
        }

        RestClient.RequestHeadersSpec<?> spec = shRssRestClient.get().uri(url);

        //Rss응답이 xml이니까 accept를 xml로 설정하기 만약 명확하지 않다? 올로 허용
        spec = spec.accept(MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.ALL);

        byte[] bytes = spec.retrieve().body(byte[].class);

        if (bytes == null || bytes.length == 0) {
            log.error("[SH][RSS] empty response. url={}", url);
            throw new IllegalStateException("SH RSS 응답이 비어있음");
        }

        log.info("[SH][RSS] fetched bytes={}", bytes.length);

        return bytes;
    }

}
