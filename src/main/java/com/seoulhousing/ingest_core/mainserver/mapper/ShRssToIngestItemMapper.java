package com.seoulhousing.ingest_core.mainserver.mapper;

import com.seoulhousing.ingest_core.external.sh.dto.ShRssItem;
import com.seoulhousing.ingest_core.mainserver.dto.AnnouncementIngestItem;
import com.seoulhousing.ingest_core.mainserver.dto.MainServerAnnouncementSource;

//Sh Rss -> 메인서버용 변경
public class ShRssToIngestItemMapper {

    private ShRssToIngestItemMapper() {}

    public static AnnouncementIngestItem map(ShRssItem src) {
        if (src == null) return null;

        return new AnnouncementIngestItem(
                MainServerAnnouncementSource.SH_RSS,     // source 고정
                trimToNull(src.getSeq()),                // externalKey

                trimToNull(src.getTitle()),              // title
                "SH",                                    // publisher
                null,                                    // housingType
                null,                                    // supplyType
                null,                                    // regionName

                null,                                    // startDate
                null,                                    // endDate
                null,                                    // documentPublishedAt
                null,                                    // finalPublishedAt

                trimToNull(src.getLink()),               // applyUrl
                null, null, null, null, null,            // 금액들 없음
                null,                                    // fullAddress
                null                                     // refrnLegaldongNm
        );
    }

    private static String trimToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
