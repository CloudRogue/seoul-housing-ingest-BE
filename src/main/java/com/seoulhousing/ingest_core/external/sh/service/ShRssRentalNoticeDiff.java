package com.seoulhousing.ingest_core.external.sh.service;

//lastSeenSeq 기준으로 신규 임대 공고가 있는지 판단 하는 클래스

import com.seoulhousing.ingest_core.external.sh.dto.ShRssDiffResult;
import com.seoulhousing.ingest_core.external.sh.dto.ShRssItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ShRssRentalNoticeDiff {

    private static final Logger log = LoggerFactory.getLogger(ShRssRentalNoticeDiff.class);

    public ShRssDiffResult diff(List<ShRssItem> items, String lastSeenSeq){
        if (items == null) throw new IllegalArgumentException("items must not be null");
        if (lastSeenSeq == null || lastSeenSeq.isBlank()) {
            throw new IllegalArgumentException("lastSeenSeq must not be null/blank");
        }

        boolean lastSeenFound = false;
        boolean hasNewRental = false;

        //lastseen 갱신용으로 사용
        String latestSeq = items.isEmpty() ? null : items.get(0).getSeq();

        for (ShRssItem item : items) {

            String seq = item.getSeq();
            String title = item.getTitle();

            if (seq == null || title == null) continue;

            //원하는 값 찾았다?? 그럼 바로 멈추기
            if (lastSeenSeq.equals(seq)) {
                lastSeenFound = true;
                break;
            }

            if (isRentalTitle(title)) {
                hasNewRental = true;
            }
        }

        //lastSeenSeq를 찾지 못했다면 Rss보관 범위 밖이거나 저장값 불일치일 가능성 높음
        if(!lastSeenFound){
            log.warn("[SH][RSS] lastSeenSeq not found. lastSeenSeq={}, latestSeq={}",
                    lastSeenSeq, latestSeq);
            return new ShRssDiffResult(false, false, latestSeq);
        }

        return new ShRssDiffResult(hasNewRental, true, latestSeq);
    }

    // 임대가 포함되면 그냥 임대공고로 본다(1차 필터링느낌)
    private static boolean isRentalTitle(String title) {
        return title.contains("임대");
    }
}
