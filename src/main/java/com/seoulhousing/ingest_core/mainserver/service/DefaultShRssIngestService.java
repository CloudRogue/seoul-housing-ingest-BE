package com.seoulhousing.ingest_core.mainserver.service;

import com.seoulhousing.ingest_core.announcement.dto.ChangeDetectionResult;
import com.seoulhousing.ingest_core.announcement.service.AnnouncementChangeDetectionService;
import com.seoulhousing.ingest_core.announcement.service.StdIdGenerator;
import com.seoulhousing.ingest_core.external.sh.dto.ShRssItem;
import com.seoulhousing.ingest_core.external.sh.service.ShRentalNoticeChecker;
import com.seoulhousing.ingest_core.mainserver.dto.AnnouncementIngestItem;
import com.seoulhousing.ingest_core.mainserver.mapper.ShRssToIngestItemMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DefaultShRssIngestService implements ShRssIngestService {

    private static final Logger log = LoggerFactory.getLogger(DefaultShRssIngestService.class);

    private static final String SOURCE = "sh";

    //원문수집 및 파싱해서 리스트주는 컴포넌트
    private final ShRentalNoticeChecker shChecker;

    private final StdIdGenerator stdIdGenerator;

    //레디스 읽어서 신규 판단
    private final AnnouncementChangeDetectionService changeDetectionService;

    @Value("${ingest.scope}")
    private String scope;

    @Value("${ingest.sh.seed-limit}")
    private int seedLimit;

    @Override
    public List<AnnouncementIngestItem> collectNewItems(String category) {

        // category 방어
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("category must not be null/blank");
        }
        String cat = category.trim();

        // RSS 전체 아이템 가져오기
        List<ShRssItem> items = shChecker.fetchAllItems();
        if (items == null) items = List.of();

        // 최신 우선으로 정렬
        List<ShRssItem> sorted = new ArrayList<>(items);
        sorted.sort(
                Comparator.comparing(
                                ShRssItem::getPublishedAt,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        )
                        .thenComparing(
                                it -> parseSeqAsLongOrNull(it == null ? null : it.getSeq()),
                                Comparator.nullsLast(Comparator.reverseOrder())
                        )
        );

        // 현재 stdId 목록 생성
        List<String> currentStdIds = new ArrayList<>();
        for (ShRssItem it : sorted) {
            if (it == null) continue;

            String seq = trimToNull(it.getSeq());
            if (seq == null) continue;

            String stdId = stdIdGenerator.shRss(seq);
            if (stdId != null && !stdId.isBlank()) {
                currentStdIds.add(stdId.trim());
            }
        }

        // 레디스 기반 신규 판단
        ChangeDetectionResult diff = changeDetectionService.detect(
                SOURCE,
                cat,
                scope,
                currentStdIds
        );

        log.info("[SH][RSS][{}] current={}, seen={}, new={}, missing={}",
                cat,
                diff.getCurrentCount(),
                diff.getSeenCount(),
                diff.getNewStdIds().size(),
                diff.getMissingStdIds().size()
        );

        // 신규 stdId set 준비
        Set<String> newStdIdSet = new HashSet<>(diff.getNewStdIds());

        // 최초실행
        boolean isFirstRun = diff.getSeenCount() == 0;

        //  신규 item만 골라서 메인서버 DTO로 변환
        List<AnnouncementIngestItem> out = new ArrayList<>();

        for (ShRssItem it : sorted) {
            if (it == null) continue;

            String seq = trimToNull(it.getSeq());
            if (seq == null) continue;

            String stdId = stdIdGenerator.shRss(seq);

            // 최초 실행이면: 신규판단을 전부신규로봄 seedLimit개만
            // 이후 실행이면: diff.newStdIds에 포함된 것만
            boolean shouldInclude = isFirstRun || (stdId != null && newStdIdSet.contains(stdId));

            if (!shouldInclude) continue;

            // 변환
            AnnouncementIngestItem mapped = ShRssToIngestItemMapper.map(it);

            // externalKey필수
            if (mapped == null || mapped.externalKey() == null || mapped.externalKey().isBlank()) {
                continue;
            }

            out.add(mapped);

            // 최초 실행이면 seedLimit개만 채우고 종료
            if (isFirstRun && out.size() >= seedLimit) break;
        }

        if (out.isEmpty()) {
            log.info("[SH][RSS][{}] no new items -> empty", cat);
            return List.of();
        }

        log.info("[SH][RSS][{}] newItemsForIngest={}", cat, out.size());
        return List.copyOf(out);
    }

    // seq 파싱 정렬용
    private static Long parseSeqAsLongOrNull(String seq) {
        String v = trimToNull(seq);
        if (v == null) return null;
        try {
            return Long.parseLong(v);
        } catch (Exception e) {
            return null;
        }
    }

    // 문자열 정규화
    private static String trimToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
