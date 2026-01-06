package com.seoulhousing.ingest_core.external.myhome.service;

import com.seoulhousing.ingest_core.external.myhome.client.MyHomeApiClient;
import com.seoulhousing.ingest_core.external.myhome.dto.LtRsdtListRequest;
import com.seoulhousing.ingest_core.external.myhome.dto.MyHomeItemDto;
import com.seoulhousing.ingest_core.external.myhome.dto.MyHomeListResponse;
import com.seoulhousing.ingest_core.external.myhome.dto.RsdtListRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class MyHomeSeoulAnnouncementCollector implements MyHomeAnnouncementCollector{

    private static final Logger log = LoggerFactory.getLogger(MyHomeSeoulAnnouncementCollector.class);

    //서울 코드
    private static final String SEOUL_BRTC_CODE = "11";

    private final MyHomeApiClient myHomeApiClient;

    public MyHomeSeoulAnnouncementCollector(MyHomeApiClient myHomeApiClient) {
        this.myHomeApiClient = myHomeApiClient;
    }

    // 공공임대  서울만 수집
    @Override
    public List<MyHomeItemDto> collectRsdt(RsdtListRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        RsdtListRequest base = request.withBrtcCode(SEOUL_BRTC_CODE);
        int numOfRows = base.getNumOfRows();

        List<MyHomeItemDto> acc = new ArrayList<>();

        long totalCount = -1L;
        long lastPage = -1L;

        for (int pageNo = 1; ; pageNo++) {
            RsdtListRequest pageReq = base.withPageNo(pageNo);

            MyHomeListResponse res = myHomeApiClient.fetchRsdt(pageReq);
            List<MyHomeItemDto> items = res.itemsOrEmpty();

            if (items.isEmpty()) break;

            acc.addAll(items);

            // totalCount는 처음 한 번만 파싱해서 lastPage 계산
            if (lastPage < 0) {
                totalCount = parseLongOrMinus1(res.totalCountOrNull());
                if (totalCount >= 0) {
                    lastPage = ceilDiv(totalCount, numOfRows);
                    if (lastPage == 0) break;
                }
            }

            // totalCount 기반 종료가 가능하면 그걸 우선시하게 메서드서렂ㅇ
            if (lastPage > 0 && pageNo >= lastPage) break;

            // 마지막페이지는 보통 덜채워져서 내려오므로 메소드설정
            if (items.size() < numOfRows) break;
    }
        log.info("[MyHome][RSDT][seoul] collected={}", acc.size());
        return acc;
    }

    // 공공분양 서울만 수집
    @Override
    public List<MyHomeItemDto> collectLtRsdt(LtRsdtListRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        LtRsdtListRequest base = request.withBrtcCode(SEOUL_BRTC_CODE);
        int numOfRows = base.getNumOfRows();

        List<MyHomeItemDto> acc = new ArrayList<>();

        long totalCount = -1L;
        long lastPage = -1L;

        for (int pageNo = 1; ; pageNo++) {
            LtRsdtListRequest pageReq = base.withPageNo(pageNo);

            MyHomeListResponse res = myHomeApiClient.fetchLtRsdt(pageReq);
            List<MyHomeItemDto> items = res.itemsOrEmpty();

            if (items.isEmpty()) break;

            acc.addAll(items);

            if (lastPage < 0) {
                totalCount = parseLongOrMinus1(res.totalCountOrNull());
                if (totalCount >= 0) {
                    lastPage = ceilDiv(totalCount, numOfRows);
                    if (lastPage == 0) break;
                }
            }

            if (lastPage > 0 && pageNo >= lastPage) break;
            if (items.size() < numOfRows) break;
        }

        log.info("[MyHome][LTRSDT][seoul] collected={}", acc.size());
        return acc;
    }



    private static long parseLongOrMinus1(String raw) {
        if (raw == null || raw.isBlank()) return -1L;
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    private static long ceilDiv(long a, long b) {
        return (a + b - 1) / b;
    }
}
