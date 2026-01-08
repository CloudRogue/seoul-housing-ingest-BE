package com.seoulhousing.ingest_core.external.sh.dto;

import lombok.Getter;

import java.util.List;

//Rss 비교 결과 Dto
@Getter
public class ShRssDiffResult {

    //신규 임대 공고가 있나
    private final boolean hasNewRental;
    private final boolean lastSeenFound;
    private final String latestSeq;
    private final List<ShRssItem> newRentalItems;

    public ShRssDiffResult(
            boolean hasNewRental,
            boolean lastSeenFound,
            String latestSeq,
            List<ShRssItem> newRentalItems
    ) {
        this.hasNewRental = hasNewRental;
        this.lastSeenFound = lastSeenFound;
        this.latestSeq = latestSeq;
        this.newRentalItems = (newRentalItems == null) // 만약 null이다??
                ? List.of() // 그럼 불변리스트
                : List.copyOf(newRentalItems); // 아니면복사해서 저장
    }
}
