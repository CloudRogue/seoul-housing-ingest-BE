package com.seoulhousing.ingest_core.external.myhome.service;

import com.seoulhousing.ingest_core.external.myhome.client.MyHomeApiClient;
import com.seoulhousing.ingest_core.external.myhome.dto.MyHomeItemDto;
import com.seoulhousing.ingest_core.external.myhome.dto.RsdtListRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class SeoulRsdtCollector implements RsdtCollector {

    private static final Logger log = LoggerFactory.getLogger(SeoulRsdtCollector.class);
    private static final String SEOUL_BRTC_CODE = "11";

    private final MyHomeApiClient client;

    public SeoulRsdtCollector(MyHomeApiClient client) {
        this.client = client;
    }

    @Override
    public List<MyHomeItemDto> collect(RsdtListRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        RsdtListRequest base = request.withBrtcCode(SEOUL_BRTC_CODE);

        // 페이지 넘버 모르고 호출
        MyHomePageCursor cursor = new MyHomePageCursor(
                pageNo -> client.fetchRsdt(base.withPageNo(pageNo))
        );

        List<MyHomeItemDto> acc = new ArrayList<>();

        // 한번 더 호출해서 만약 빈값이다?? 그럼 종료
        List<MyHomeItemDto> items;
        while ((items = cursor.next()) != null) {
            acc.addAll(items);
        }

        log.info("[MyHome][RSDT][seoul] collected={}", acc.size());
        return acc;
    }
}
