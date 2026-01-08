package com.seoulhousing.ingest_core.external.sh.service;

//lastSeenSeq 기준으로 신규 임대 공고가 있는지 판단 하는 클래스

import com.seoulhousing.ingest_core.external.sh.dto.ShRssDiffResult;
import com.seoulhousing.ingest_core.external.sh.dto.ShRssItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class ShRssRentalNoticeDiff {

    private static final Logger log = LoggerFactory.getLogger(ShRssRentalNoticeDiff.class);

    public ShRssDiffResult diff(List<ShRssItem> items, String lastSeenSeq){
        if (items == null) throw new IllegalArgumentException("items must not be null");
        if (lastSeenSeq == null || lastSeenSeq.isBlank()) {
            throw new IllegalArgumentException("lastSeenSeq must not be null/blank");
        }

        // 정렬 보장하기 pubdate 내림차순으로 그다음 seq 만약 pubDate가 널이면 제일 뒤로 보냄
        Comparator<java.time.Instant> pubDescNullLast =
                Comparator.nullsLast(Comparator.reverseOrder());

        Comparator<Long> seqDescNullLast =
                Comparator.nullsLast(Comparator.reverseOrder());

        List<ShRssItem> sorted = new ArrayList<>(items);
        sorted.sort(
                Comparator.comparing(ShRssItem::getPublishedAt, pubDescNullLast)
                        .thenComparing(item -> parseSeqAsLongOrNull(item.getSeq()), seqDescNullLast)
        );


        String latestSeq = sorted.isEmpty() ? null : sorted.get(0).getSeq();

        // lastSeenSeq를 만날때까지검사
        boolean lastSeenFound = false;
        List<ShRssItem> newRentalItems = new ArrayList<>();

        for (ShRssItem item : sorted) {
            String seq = item.getSeq();
            String title = item.getTitle();
            String link = item.getLink();

            if (seq == null || seq.isBlank()) continue;
            if (title == null || title.isBlank()) continue;
            if (link == null || link.isBlank()) continue;

            if (lastSeenSeq.equals(seq)) {
                lastSeenFound = true;
                break;
            }

            if(isRentalTitle(title)) {
                newRentalItems.add(item);
            }

        }
        if (!lastSeenFound) { // lastSeenSeq를 끝까지 못 찾은 경우 로그로 띄워주기
            log.warn("[SH][RSS] lastSeenSeq not found. lastSeenSeq={}, latestSeq={}, items={}",
                    lastSeenSeq,
                    latestSeq,
                    sorted.size());
            return new ShRssDiffResult(false, false, latestSeq, List.of());
        }

        boolean hasNewRental = !newRentalItems.isEmpty();

        return new ShRssDiffResult(hasNewRental, true, latestSeq, newRentalItems);





    }

    // 임대가 포함되면 그냥 임대공고로 본다(1차 필터링느낌)
    private static boolean isRentalTitle(String title) {
        return title.contains("임대");
    }

    private static Long parseSeqAsLongOrNull(String seq) {
        try {
            return Long.parseLong(seq);
        } catch (Exception e) {
            return null;
        }
    }
}
