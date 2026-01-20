package com.seoulhousing.ingest_core.external.myhome.service;

import com.seoulhousing.ingest_core.external.myhome.client.MyHomeApiClient;
import com.seoulhousing.ingest_core.external.myhome.dto.LtRsdtListRequest;
import com.seoulhousing.ingest_core.external.myhome.dto.MyHomeItemDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class SeoulLtRsdtCollector implements LtRsdtCollector {

    private static final Logger log = LoggerFactory.getLogger(SeoulLtRsdtCollector.class);
    private static final String SEOUL_BRTC_CODE = "11";

    private final MyHomeApiClient client;

    public SeoulLtRsdtCollector(MyHomeApiClient client) {
        this.client = client;
    }

    @Override
    public List<MyHomeItemDto> collect(LtRsdtListRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        LtRsdtListRequest base = request.withBrtcCode(SEOUL_BRTC_CODE);

        MyHomePageCursor cursor = new MyHomePageCursor(
                pageNo -> client.fetchLtRsdt(base.withPageNo(pageNo))
        );

        List<MyHomeItemDto> acc = new ArrayList<>();

        List<MyHomeItemDto> items;
        while ((items = cursor.next()) != null) {
            acc.addAll(items);
        }

        log.info("[MyHome][LTRSDT][seoul] collected={}", acc.size());
        return acc;
    }

}
