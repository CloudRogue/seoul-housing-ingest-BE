package com.seoulhousing.ingest_core.announcement.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.time.Instant;

@Getter
@RequiredArgsConstructor
@ToString
public class LhAnnouncementPayload {

    private final String stdId;
    private final String category;
    private final String scope;

    private final String title;
    private final String link;
    private final Instant publishedAt;

    // LH 전용
    private final String beginDe;
    private final String endDe;

    private final String bodyJson;
}
