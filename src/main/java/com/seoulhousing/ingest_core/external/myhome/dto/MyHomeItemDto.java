package com.seoulhousing.ingest_core.external.myhome.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class MyHomeItemDto {

    // ===== 식별/상태 =====
    private String pblancId;       // 공고ID
    private String houseSn;        // 주택 일련번호
    private String sttusNm;        // 상태_명
    private String beforePblancId; // 이전_공고_ID

    // ===== 공고 기본 =====
    private String pblancNm;       // 공고명
    private String suplyInsttNm;   // 공급_기관명
    private String houseTyNm;      // 주택유형_명
    private String suplyTyNm;      // 공급유형명 (rsdt에 존재)

    // ===== 일정 =====
    private String rcritPblancDe;      // 모집공고일자
    private String przwnerPresnatnDe;  // 당첨자발표일자
    private String beginDe;            // 모집 시작 일자
    private String endDe;              // 모집 종료 일자

    // ===== 링크/문의 =====
    private String refrnc;     // 문의처
    private String url;        // 모집공고 URL
    private String pcUrl;      // 마이홈포털 PC URL
    private String mobileUrl;  // 마이홈포털 Mobile URL

    // ===== 위치/주소 =====
    private String hsmpNm;            // 단지_명
    private String brtcNm;            // 광역시도명
    private String signguNm;          // 시군구명
    private String fullAdres;         // 전체주소
    private String rnCodeNm;          // 도로명
    private String refrnLegaldongNm;  // 참조_법정동명
    private String pnu;               // PNU

    // ===== 기타 =====
    private String heatMthdNm;   // 난방_방식명
    private String totHshldCo;   // 총세대수 (rsdt에 존재)

    // ===== 공급/금액 (rsdt에 존재) =====
    private String suplyHoCo;    // 공급호수(전세임대 해당)
    private String sumSuplyCo;   // 공급호수 (명세: number)
    private String rentGtn;      // 최소임대보증금 (명세: number)
    private String enty;         // 최소_계약금 (명세: number)
    private String prtpay;       // 최소_중도금 (명세: number)
    private String surlus;       // 최소_잔금 (명세: number)
    private String mtRntchrg;    // 최소월임대료 (명세: number)
}