package com.seoulhousing.ingest_core.mainserver.service;

import com.seoulhousing.ingest_core.mainserver.dto.AnnouncementIngestItem;

import java.util.List;

//Sh Rss 수집 -> 신규만 추리고 ->메인서버 전송용 dto로 변환
public interface ShRssIngestService {

    List<AnnouncementIngestItem> collectNewItems(String category);
}
