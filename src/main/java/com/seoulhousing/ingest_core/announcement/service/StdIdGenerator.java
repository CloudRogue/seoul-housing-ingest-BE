package com.seoulhousing.ingest_core.announcement.service;

public interface StdIdGenerator {

    // MyHome 표준 Id 생성
    String myhome(String category, String pblancId, String houseSn);

    // SH Rss 표준 Id 생성
    String shRss(String seq);
}
