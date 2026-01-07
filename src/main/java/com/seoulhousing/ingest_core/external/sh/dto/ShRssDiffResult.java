package com.seoulhousing.ingest_core.external.sh.dto;

import lombok.Getter;

//Rss 비교 결과 Dto
@Getter
public class ShRssDiffResult {

    //신규 임대 공고가 있나
    private final boolean hasNewRental;
    private final boolean lastSeenFound;
    private final String latestSeq;

    public ShRssDiffResult(boolean hasNewRental, boolean lastSeenFound, String latestSeq) {
        this.hasNewRental = hasNewRental;
        this.lastSeenFound = lastSeenFound;
        this.latestSeq = latestSeq;
    }
}
