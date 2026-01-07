package com.seoulhousing.ingest_core.external.sh.service;

import com.seoulhousing.ingest_core.external.sh.dto.ShRssDiffResult;

public interface ShRentalNoticeChecker {

    //신규 임대 공고 존재 여부확인
    ShRssDiffResult checkNewRentalNotice(String lastSeenSeq);
}
