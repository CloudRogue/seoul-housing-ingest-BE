package com.seoulhousing.ingest_core.external.sh.service;

import com.seoulhousing.ingest_core.external.sh.client.ShRssApiClient;
import com.seoulhousing.ingest_core.external.sh.dto.ShRssItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShRentalNoticeCheckerService implements ShRentalNoticeChecker{

    private static final Logger log = LoggerFactory.getLogger(ShRentalNoticeCheckerService.class);

    private final ShRssApiClient client;
    private final ShRssXmlParser parser;


    public ShRentalNoticeCheckerService(ShRssApiClient client, ShRssXmlParser parser) {
        this.client = client;
        this.parser = parser;
    }


    @Override
    public List<ShRssItem> fetchAllItems() {
        //원문가져오기
        byte[] rssBytes = client.fetchNoticeRssBytes();

        //xml 파싱 하고 아이템리스트
        List<ShRssItem> items = parser.parse(rssBytes);

        if (items == null) items = List.of();

        log.info("[SH][RSS] fetched items={}", items.size());
        return items;
    }
}
