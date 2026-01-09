package com.seoulhousing.ingest_core.announcement.service;

import org.springframework.stereotype.Component;

@Component
public class DefaultStdIdGenerator implements StdIdGenerator {

    private static final String MYHOME_PREFIX = "myhome";
    private static final String SH_RSS_PREFIX = "sh:rss";

    @Override
    public String myhome(String category, String pblancId, String houseSn) {
        return MYHOME_PREFIX + req(category,"category") + ":" + req(pblancId,"pblancId") + ":" + req(houseSn,"houseSn");
    }

    @Override
    public String shRss(String seq) {
        return SH_RSS_PREFIX + req(seq,"seq");
    }


    private static String req(String v, String name) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(name + " is blank");
        return v.trim();
    }
}
