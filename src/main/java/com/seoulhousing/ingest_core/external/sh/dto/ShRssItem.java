package com.seoulhousing.ingest_core.external.sh.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 파싱 단계에서 원하는 값만을 추출하기 위해서 사용
@Getter
public class ShRssItem {

    private final String seq;
    private final String title;
    private final String link;

    public ShRssItem(String seq, String title, String link) {
        this.seq = seq;
        this.title = title;
        this.link = link;
    }
}
