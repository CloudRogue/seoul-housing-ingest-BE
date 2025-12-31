package com.seoulhousing.ingest_core.external.myhome.dto;

import com.seoulhousing.ingest_core.external.myhome.client.MyHomeApiClient;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

@Getter
public class MyHomeListRequest {

    private final int pageNo;
    private final int numOfRows;

    // 공통(임대/분양)
    private final String brtcCode;     // 광역시도 코드
    private final String signguCode;   // 시군구 코드
    private final String houseTy;      // 주택유형
    private final String yearMtBegin;  // 모집공고월 시작(YYYYMM)
    private final String yearMtEnd;    // 모집공고월 종료(YYYYMM)

    // 임대(RSDT) 전용
    private final String suplyTy;           // 공급유형
    private final String lfstsTyAt;         // 전세형 모집 여부
    private final String bassMtRntchrgSe;   // 월임대료 구분

    @Builder
    public MyHomeListRequest(
            int pageNo,
            int numOfRows,
            String brtcCode,
            String signguCode,
            String houseTy,
            String yearMtBegin,
            String yearMtEnd,
            String suplyTy,
            String lfstsTyAt,
            String bassMtRntchrgSe
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

        this.suplyTy = normalizeBlankToNull(suplyTy);
        this.lfstsTyAt = normalizeBlankToNull(lfstsTyAt);
        this.bassMtRntchrgSe = normalizeBlankToNull(bassMtRntchrgSe);
    }

    public MultiValueMap<String, String> toQueryParams(MyHomeApiClient.Category category) {
        MultiValueMap<String, String> q = new LinkedMultiValueMap<>();
        q.add("pageNo", String.valueOf(pageNo));
        q.add("numOfRows", String.valueOf(numOfRows));

        // 공통
        putIfText(q, "brtcCode", brtcCode);
        putIfText(q, "signguCode", signguCode);
        putIfText(q, "houseTy", houseTy);
        putIfText(q, "yearMtBegin", yearMtBegin);
        putIfText(q, "yearMtEnd", yearMtEnd);

        // 임대전용 파라미터만 추가
        if (category == MyHomeApiClient.Category.RSDT) {
            putIfText(q, "suplyTy", suplyTy);
            putIfText(q, "lfstsTyAt", lfstsTyAt);
            putIfText(q, "bassMtRntchrgSe", bassMtRntchrgSe);
        }

        return q;
    }

    private static void putIfText(MultiValueMap<String, String> q, String key, String value) {
        if (StringUtils.hasText(value)) q.add(key, value);
    }

    private static String normalizeBlankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }
}