package com.seoulhousing.ingest_core.external.sh.service;

import com.seoulhousing.ingest_core.external.sh.dto.ShRssItem;

import java.util.List;

public interface ShRentalNoticeChecker {

    // Sh rss 전체아이템을 파싱하고 반환
    List<ShRssItem> fetchAllItems();
}
