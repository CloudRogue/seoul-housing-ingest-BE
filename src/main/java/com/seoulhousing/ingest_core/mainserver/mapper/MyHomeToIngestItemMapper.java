package com.seoulhousing.ingest_core.mainserver.mapper;


import com.seoulhousing.ingest_core.external.myhome.dto.MyHomeItemDto;
import com.seoulhousing.ingest_core.mainserver.dto.AnnouncementIngestItem;
import com.seoulhousing.ingest_core.mainserver.dto.MainServerAnnouncementSource;

import java.time.LocalDate;

public final class MyHomeToIngestItemMapper {

    private MyHomeToIngestItemMapper() {}

    public static AnnouncementIngestItem map(MyHomeItemDto myHomeItemDto) {
        if (myHomeItemDto == null) return null;

        String externalKey = joinKey(myHomeItemDto.getPblancId(),myHomeItemDto.getHouseSn());

        return new AnnouncementIngestItem(
                MainServerAnnouncementSource.MYHOME,                 // source 고정
                externalKey,                                         // 외부식별자

                trimToNull(myHomeItemDto.getPblancNm()),
                trimToNull(myHomeItemDto.getSuplyInsttNm()),
                trimToNull(myHomeItemDto.getHouseTyNm()),
                trimToNull(myHomeItemDto.getSuplyTyNm()),
                trimToNull(myHomeItemDto.getSignguNm()),

                parseDateOrNull(myHomeItemDto.getBeginDe()),
                parseDateOrNull(myHomeItemDto.getEndDe()),
                null,                                 // 서류합격발표날짜
                parseDateOrNull(myHomeItemDto.getPrzwnerPresnatnDe()),

                trimToNull(myHomeItemDto.getUrl()),
                parseLongOrNull(myHomeItemDto.getRentGtn()),
                parseLongOrNull(myHomeItemDto.getEnty()),
                parseLongOrNull(myHomeItemDto.getPrtpay()),
                parseLongOrNull(myHomeItemDto.getSurlus()),
                parseLongOrNull(myHomeItemDto.getMtRntchrg()),

                trimToNull(myHomeItemDto.getFullAdres()),
                trimToNull(myHomeItemDto.getRefrnLegaldongNm())
        );
    }

    private static String joinKey(String pblancId, String houseSn) {
        String a = trimToNull(pblancId);
        String b = trimToNull(houseSn);
        if (a == null || b == null) return null;
        return a + ":" + b;
    }

    // 공백제거
    private static String trimToNull(String s) {
        if (s == null) return null;
        String trim = s.trim();
        return trim.isEmpty() ? null : trim;
    }

    //  LocalDate로 변형
    private static LocalDate parseDateOrNull(String yyyymmdd) {
        String v = trimToNull(yyyymmdd);
        if (v == null) return null;

        if (v.length() != 8) return null;

        try {
            int y = Integer.parseInt(v.substring(0, 4));
            int m = Integer.parseInt(v.substring(4, 6));
            int d = Integer.parseInt(v.substring(6, 8));
            return LocalDate.of(y, m, d);
        } catch (Exception e) {
            return null;
        }
    }

    //숫자 문자열 -> Long
    private static Long parseLongOrNull(String raw) {
        String v = trimToNull(raw);
        if (v == null) return null;
        try {
            return Long.parseLong(v);
        } catch (Exception e) {
            return null;
        }
    }

}
