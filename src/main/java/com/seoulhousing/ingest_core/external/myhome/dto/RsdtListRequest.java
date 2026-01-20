package com.seoulhousing.ingest_core.external.myhome.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

@Getter
public class RsdtListRequest {

    private final int pageNo;
    private final int numOfRows;

    private final String brtcCode;
    private final String signguCode;
    private final String houseTy;
    private final String yearMtBegin;
    private final String yearMtEnd;

    // 임대 전용
    private final String suplyTy;
    private final String lfstsTyAt;
    private final String bassMtRntchrgSe;

    @Builder
    public RsdtListRequest(
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

    public MultiValueMap<String, String> toQueryParams() {
        MultiValueMap<String, String> q = new LinkedMultiValueMap<>();
        q.add("pageNo", String.valueOf(pageNo));
        q.add("numOfRows", String.valueOf(numOfRows));

        putIfText(q, "brtcCode", brtcCode);
        putIfText(q, "signguCode", signguCode);
        putIfText(q, "houseTy", houseTy);
        putIfText(q, "yearMtBegin", yearMtBegin);
        putIfText(q, "yearMtEnd", yearMtEnd);

        putIfText(q, "suplyTy", suplyTy);
        putIfText(q, "lfstsTyAt", lfstsTyAt);
        putIfText(q, "bassMtRntchrgSe", bassMtRntchrgSe);

        return q;
    }


    //파라미터를 안전하게 추가할수 있도록
    private static void putIfText(MultiValueMap<String, String> q, String key, String value) {
        if (StringUtils.hasText(value)) q.add(key, value);
    }


    //입력값을 통일해주기 위한 정규화 메서드
    private static String normalizeBlankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }


    // 페이지 넘버 바꿔주는 메서드
    public RsdtListRequest withPageNo(int pageNo) {
        return new RsdtListRequest(
                pageNo,
                this.numOfRows,
                this.brtcCode,
                this.signguCode,
                this.houseTy,
                this.yearMtBegin,
                this.yearMtEnd,
                this.suplyTy,
                this.lfstsTyAt,
                this.bassMtRntchrgSe
        );
    }

    public RsdtListRequest withBrtcCode(String brtcCode) {
        return new RsdtListRequest(
                this.pageNo,
                this.numOfRows,
                brtcCode,
                this.signguCode,
                this.houseTy,
                this.yearMtBegin,
                this.yearMtEnd,
                this.suplyTy,
                this.lfstsTyAt,
                this.bassMtRntchrgSe
        );
    }
}
