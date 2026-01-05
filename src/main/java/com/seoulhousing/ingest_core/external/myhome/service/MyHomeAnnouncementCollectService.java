package com.seoulhousing.ingest_core.external.myhome.service;


import com.seoulhousing.ingest_core.external.myhome.client.MyHomeApiClient;
import com.seoulhousing.ingest_core.external.myhome.dto.LtRsdtListRequest;
import com.seoulhousing.ingest_core.external.myhome.dto.MyHomeItemDto;
import com.seoulhousing.ingest_core.external.myhome.dto.MyHomeListResponse;
import com.seoulhousing.ingest_core.external.myhome.dto.RsdtListRequest;
import com.seoulhousing.ingest_core.external.myhome.service.filter.RegionFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MyHomeAnnouncementCollectService {

    private static final Logger log = LoggerFactory.getLogger(MyHomeAnnouncementCollectService.class);

    private final MyHomeApiClient myHomeApiClient;

    public MyHomeAnnouncementCollectService(MyHomeApiClient myHomeApiClient) {
        this.myHomeApiClient = myHomeApiClient;
    }

    // 공공임대 수집
    public List<MyHomeItemDto> collectRsdt(RsdtListRequest baseRequest, RegionFilter filter) {
        Objects.requireNonNull(baseRequest, "baseRequest must not be null");
        Objects.requireNonNull(filter, "filter must not be null");

        // 전체 수집하기
        List<MyHomeItemDto> all = fetchAllRsdtPages(baseRequest);

        // 필터 적용해주기
        List<MyHomeItemDto> filtered = all.stream()
                .filter(filter::matches)
                .toList();

        // 중복 제거하기
        List<MyHomeItemDto> deduped = dedupByStdId(filtered, "RSDT");

        log.info("[MyHome][RSDT] collected={} filtered={} deduped={}",
                all.size(), filtered.size(), deduped.size());

        return deduped;
    }

    // 공공분양 수집
    public List<MyHomeItemDto> collectLtRsdt(LtRsdtListRequest baseRequest, RegionFilter filter) {
        Objects.requireNonNull(baseRequest, "baseRequest must not be null");
        Objects.requireNonNull(filter, "filter must not be null");

        List<MyHomeItemDto> all = fetchAllLtRsdtPages(baseRequest);

        List<MyHomeItemDto> filtered = all.stream()
                .filter(filter::matches)
                .toList();

        List<MyHomeItemDto> deduped = dedupByStdId(filtered, "LTRSDT");

        log.info("[MyHome][LTRSDT] collected={} filtered={} deduped={}",
                all.size(), filtered.size(), deduped.size());

        return deduped;
    }

    // 전체 수집하는 루프
    private List<MyHomeItemDto> fetchAllRsdtPages(RsdtListRequest base) {
        List<MyHomeItemDto> acc = new ArrayList<>();

        // 페이지 시작 숫자 및 몇개 가져올지 최대페이지(무한루프 방지를 위해서) 지정
        int pageNo = 1;
        int numOfRows = base.getNumOfRows();
        final int maxPages = 200;

        while (true) {
            // 불변객체를 빌더 패턴으로 재구성
            RsdtListRequest req = RsdtListRequest.builder()
                    .pageNo(pageNo)
                    .numOfRows(numOfRows)
                    .brtcCode(base.getBrtcCode())
                    .signguCode(base.getSignguCode())
                    .houseTy(base.getHouseTy())
                    .yearMtBegin(base.getYearMtBegin())
                    .yearMtEnd(base.getYearMtEnd())

                    // rsdt 전용 조건
                    .suplyTy(base.getSuplyTy())
                    .lfstsTyAt(base.getLfstsTyAt())
                    .bassMtRntchrgSe(base.getBassMtRntchrgSe())
                    .build();

            // 외부 호출은 client로 위임
            MyHomeListResponse res = myHomeApiClient.fetchRsdt(req);

            // items 안전하게 확보해주기
            List<MyHomeItemDto> items = res.itemsOrEmpty();
            acc.addAll(items);

            // 빈 배열이면 종료
            if (items.isEmpty()) break;

            // totalCount 기반 종료
            int totalCount = parseIntOrMinus1(res.totalCountOrNull());
            if (totalCount > -1) {
                int fetched = pageNo * numOfRows;
                if (fetched >= totalCount) break;
            }

            // 마지막 페이지는 보통 원래 개수보다 부족하니까 아래와 같은 코드 작성하였음
            if (items.size() < numOfRows) break;

            // 무한루프 방지하기
            if (pageNo >= maxPages) {
                log.error("[MyHome][RSDT] paging aborted: exceeded maxPages={}", maxPages);
                break;
            }

            pageNo++;
        }

        return acc;
    }

    private List<MyHomeItemDto> fetchAllLtRsdtPages(LtRsdtListRequest base) {
        List<MyHomeItemDto> acc = new ArrayList<>();

        int pageNo = 1;
        int numOfRows = base.getNumOfRows();
        final int maxPages = 200;

        while (true) {
            LtRsdtListRequest req = LtRsdtListRequest.builder()
                    .pageNo(pageNo)
                    .numOfRows(numOfRows)
                    .brtcCode(base.getBrtcCode())
                    .signguCode(base.getSignguCode())
                    .houseTy(base.getHouseTy())
                    .yearMtBegin(base.getYearMtBegin())
                    .yearMtEnd(base.getYearMtEnd())
                    .build();

            MyHomeListResponse res = myHomeApiClient.fetchLtRsdt(req);

            List<MyHomeItemDto> items = res.itemsOrEmpty();
            acc.addAll(items);

            if (items.isEmpty()) break;

            int totalCount = parseIntOrMinus1(res.totalCountOrNull());
            if (totalCount > -1) {
                int fetched = pageNo * numOfRows;
                if (fetched >= totalCount) break;
            }

            if (items.size() < numOfRows) break;

            if (pageNo >= maxPages) {
                log.error("[MyHome][LTRSDT] paging aborted: exceeded maxPages={}", maxPages);
                break;
            }

            pageNo++;
        }

        return acc;
    }

    //스트링으로 오는 값 처리
    private static int parseIntOrMinus1(String s) {
        if (s == null || s.isBlank()) return -1;
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return -1;
        }
    }

    // 혹시 모를 중복 방어해주기
    private List<MyHomeItemDto> dedupByStdId(List<MyHomeItemDto> items, String category) {
        Map<String, MyHomeItemDto> map = new LinkedHashMap<>(); // 순서 유지

        for (MyHomeItemDto it : items) {
            if (it == null) continue;

            String pblancId = safe(it.getPblancId());
            String houseSn = safe(it.getHouseSn());

            // pblancId 없으면 데이터 자체가 깨진 것이므로 그만 남기고 스킵해주기
            if (pblancId.isBlank()) {
                log.warn("[MyHome][{}] missing pblancId. item={}", category, it);
                continue;
            }

            if (houseSn.isBlank()) {
                log.warn("[MyHome][{}] missing houseSn. skip. pblancId={}, brtc={}, signgu={}",
                        category, pblancId, safe(it.getBrtcNm()), safe(it.getSignguNm()));
                continue;
            }

            String stdId = "myhome:" + category.toLowerCase() + ":" + pblancId + ":" + houseSn;

            MyHomeItemDto prev = map.putIfAbsent(stdId, it);
            if (prev != null) {
                // 중복이면 지역 정보가 다른지 로그로 찍기
                log.warn("[MyHome][{}] duplicate stdId detected. stdId={}, brtc(prev={}, now={}), signgu(prev={}, now={})",
                        category,
                        stdId,
                        safe(prev.getBrtcNm()), safe(it.getBrtcNm()),
                        safe(prev.getSignguNm()), safe(it.getSignguNm())
                );
            }
        }

        return new ArrayList<>(map.values());
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
