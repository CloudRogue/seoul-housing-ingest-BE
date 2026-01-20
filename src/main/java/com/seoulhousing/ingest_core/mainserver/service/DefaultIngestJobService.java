package com.seoulhousing.ingest_core.mainserver.service;

import com.seoulhousing.ingest_core.announcement.dto.ChangeDetectionResult;
import com.seoulhousing.ingest_core.announcement.service.AnnouncementChangeDetectionService;
import com.seoulhousing.ingest_core.announcement.service.StdIdGenerator;
import com.seoulhousing.ingest_core.external.myhome.dto.LtRsdtListRequest;
import com.seoulhousing.ingest_core.external.myhome.dto.MyHomeItemDto;
import com.seoulhousing.ingest_core.external.myhome.dto.RsdtListRequest;
import com.seoulhousing.ingest_core.external.myhome.service.LtRsdtCollector;
import com.seoulhousing.ingest_core.external.myhome.service.RsdtCollector;
import com.seoulhousing.ingest_core.external.sh.dto.ShRssItem;
import com.seoulhousing.ingest_core.mainserver.client.MainServerIngestClient;
import com.seoulhousing.ingest_core.mainserver.dto.AnnouncementIngestItem;
import com.seoulhousing.ingest_core.mainserver.dto.AnnouncementIngestRequest;
import com.seoulhousing.ingest_core.mainserver.dto.IngestResponse;
import com.seoulhousing.ingest_core.mainserver.mapper.MyHomeToIngestItemMapper;
import com.seoulhousing.ingest_core.mainserver.mapper.ShRssToIngestItemMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DefaultIngestJobService implements IngestJobService {

    private static final Logger log = LoggerFactory.getLogger(DefaultIngestJobService.class);

    //마이홈 수집기
    private final RsdtCollector rsdtCollector;
    private final LtRsdtCollector ltRsdtCollector;

    private final ShRssIngestService shRssIngestService;

    private final StdIdGenerator stdIdGenerator;
    private final AnnouncementChangeDetectionService changeDetectionService;
    private final MainServerIngestClient mainServerIngestClient;

    @Value("${ingest.scope}")
    private String scope;

    @Value("${ingest.myhome.num-of-rows}")
    private int myhomeNumOfRows;

    @Value("${ingest.myhome.category-rsdt}") // 메인서버 카테고리값
    private String myhomeRsdtCategory;

    @Value("${ingest.myhome.category-ltrsdt:ltrsdt}")
    private String myhomeLtRsdtCategory;

    @Value("${ingest.sh.category:rental}")
    private String shCategory;


    @Override
    public void runOnce() {
        // 작업 시작 로그
        log.info("[JOB] start. scope={}", scope);

        // 1.마이홈 공공임대 실행
        runMyHome(myhomeRsdtCategory, collectRsdt());

        // 2.마이홈 공공분양 실행
        runMyHome(myhomeLtRsdtCategory, collectLtRsdt());

        // 3.Sh RSS 실행
        runShRss(shCategory);

        // 작업 종료 로그
        log.info("[JOB] done.");

    }


    //마이홈 수집 공공임대
    private List<MyHomeItemDto> collectRsdt() {
        // 요청 생성
        RsdtListRequest req = RsdtListRequest.builder()
                .pageNo(1)
                .numOfRows(myhomeNumOfRows)
                .build();

        //수집실행
        List<MyHomeItemDto> items = rsdtCollector.collect(req);
        if (items == null) items = List.of();
        log.info("[MyHome][{}] collected={}", myhomeRsdtCategory, items.size());
        return items;
    }

    //마이홈 수집 공공분양
    private List<MyHomeItemDto> collectLtRsdt() {
        // 요청 생성
        LtRsdtListRequest req = LtRsdtListRequest.builder()
                .pageNo(1)
                .numOfRows(myhomeNumOfRows)
                .build();

        //수집실행
        List<MyHomeItemDto> items = ltRsdtCollector.collect(req);
        if (items == null) items = List.of();
        log.info("[MyHome][{}] collected={}", myhomeLtRsdtCategory, items.size());
        return items;
    }


    //마이홈 카테고리 단위 실행
    private void runMyHome(String category, List<MyHomeItemDto> items) {
        if(items == null) items = List.of();

        //current stdId 생성
        List<String> currentStdIds = new ArrayList<>(); // 현재 stdId 리스트
        for (MyHomeItemDto it : items) {
            if (it == null) continue;
            String stdId = stdIdGenerator.myhomeOrNull(category, it.getPblancId(), it.getHouseSn());
            if (stdId != null && !stdId.isBlank()) currentStdIds.add(stdId.trim());
        }

        // 신규 감지
        ChangeDetectionResult diff = changeDetectionService.detect(
                "myhome",
                category,
                scope,
                currentStdIds // current list
        );

        // 결과 로그
        log.info("[MyHome][{}] current={}, seen={}, new={}, missing={}",
                category, // 카테고리
                diff.getCurrentCount(), // 현재 개수
                diff.getSeenCount(), // seen 개수
                diff.getNewStdIds().size(), // 신규
                diff.getMissingStdIds().size() // 누락
        );

        //신규 stdId set 생성
        Set<String> newStdIdSet = new HashSet<>(diff.getNewStdIds());

        //신규 아이템만 추려서 메인서버dto로 변환
        List<AnnouncementIngestItem> newIngestItems = new ArrayList<>(); // 신규 전송 리스트

        for (MyHomeItemDto it : items) {
            if (it == null) continue;

            String stdId = stdIdGenerator.myhomeOrNull(category, it.getPblancId(), it.getHouseSn());
            if (stdId == null || !newStdIdSet.contains(stdId)) continue;

            AnnouncementIngestItem mapped = MyHomeToIngestItemMapper.map(it);
            if (mapped == null || mapped.externalKey() == null || mapped.externalKey().isBlank()) continue;

            newIngestItems.add(mapped);
        }

        //신규가없으면 메인서버 호출 스킵
        if (newIngestItems.isEmpty()) {
            log.info("[MyHome][{}] no new -> skip ingest", category);
            return;
        }

        //신규가 있으면 메인서버 호출
        AnnouncementIngestRequest req = new AnnouncementIngestRequest(
                category,
                newIngestItems
        );
        IngestResponse res = mainServerIngestClient.ingest(req);

        //결과 로그
        log.info("[MyHome][{}] ingest result. received={}, created={}, updated={}, skipped={}",
                category, res.received(), res.created(), res.updated(), res.skipped());
    }

    //Sh rss 실행
    private void runShRss(String category) {

        // category 방어
        if (category == null || category.isBlank()) {
            log.warn("[SH][RSS] category is blank -> skip");
            return;
        }

        // 신규만 추려서 ingest item 리스트를 만든다
        List<AnnouncementIngestItem> ingestItems = shRssIngestService.collectNewItems(category);

        // 신규 없으면 호출 스킵
        if (ingestItems == null || ingestItems.isEmpty()) {
            log.info("[SH][RSS][{}] no new -> skip ingest", category);
            return;
        }

        //  메인서버 요청 DTO 생성
        AnnouncementIngestRequest req = new AnnouncementIngestRequest(
                category,
                ingestItems
        );

        //  메인서버 호출
        IngestResponse res = mainServerIngestClient.ingest(req);

        // 결과 로그
        log.info("[SH][RSS][{}] ingest result. received={}, created={}, updated={}, skipped={}",
                category, res.received(), res.created(), res.updated(), res.skipped());
    }
}
