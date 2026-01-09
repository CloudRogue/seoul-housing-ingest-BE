package com.seoulhousing.ingest_core.announcement.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@Getter
@RequiredArgsConstructor
public class StdAnnouncement {

    private final String stdId;
    private final String source;
    private final String category;
    private final String title;
    private final String link;
    private final Instant publishedAt;

    //마이 홈에서만 존재하는 값들
    private final String beginDe;
    private final String endDe;

    @Override
    public String toString() {
        return "StdAnnouncement{" +
                "stdId='" + stdId + '\'' +
                ", source='" + source + '\'' +
                ", category='" + category + '\'' +
                ", title='" + title + '\'' +
                ", link='" + link + '\'' +
                ", publishedAt=" + publishedAt +
                ", beginDe='" + beginDe + '\'' +
                ", endDe='" + endDe + '\'' +
                '}';
    }
}
