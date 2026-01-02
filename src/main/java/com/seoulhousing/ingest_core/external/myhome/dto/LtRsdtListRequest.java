package com.seoulhousing.ingest_core.external.myhome.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

@Getter
public class LtRsdtListRequest {

    private final int pageNo;
    private final int numOfRows;


    private final String brtcCode;
    private final String signguCode;
    private final String houseTy;
    private final String yearMtBegin;
    private final String yearMtEnd;

    @Builder
    public LtRsdtListRequest(
            int pageNo,
            int numOfRows,
            String brtcCode,
            String signguCode,
            String houseTy,
            String yearMtBegin,
            String yearMtEnd
    ) {
        if (pageNo < 1) throw new IllegalArgumentException("pageNo must be >= 1");
        if (numOfRows < 1) throw new IllegalArgumentException("numOfRows must be >= 1");

        this.pageNo = pageNo;
        this.numOfRows = numOfRows;

        this.brtcCode = normalizeBlankToNull(brtcCode);
        this.signguCode = normalizeBlankToNull(signguCode);
        this.houseTy = normalizeBlankToNull(houseTy);
        this.yearMtBegin = normalizeBlankToNull(yearMtBegin);
        this.yearMtEnd = normalizeBlankToNull(yearMtEnd);
    }

    public MultiValueMap<String, String> toQueryParams() {
        MultiValueMap<String, String> q = new LinkedMultiValueMap<>();
        q.add("pageNo", String.valueOf(pageNo));
        q.add("numOfRows", String.valueOf(numOfRows));

        putIfText(q, "brtcCode", brtcCode);
        putIfText(q, "signguCode", signguCode);
        putIfText(q, "houseTy", houseTy);
        putIfText(q, "yearMtBegin", yearMtBegin);
        putIfText(q, "yearMtEnd", yearMtEnd);

        return q;
    }

    private static void putIfText(MultiValueMap<String, String> q, String key, String value) {
        if (StringUtils.hasText(value)) q.add(key, value);
    }

    private static String normalizeBlankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }
}
