package com.seoulhousing.ingest_core.external.myhome.service;

import com.seoulhousing.ingest_core.config.ExternalMyHomeProperties;
import com.seoulhousing.ingest_core.external.myhome.client.MyHomeApiClient;
import com.seoulhousing.ingest_core.external.myhome.dto.LtRsdtListRequest;
import com.seoulhousing.ingest_core.external.myhome.dto.MyHomeItemDto;
import com.seoulhousing.ingest_core.external.myhome.dto.RsdtListRequest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;


import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("it")
class MyHomeLocalSmokeIT {

    @Autowired
    SeoulRsdtCollector rsdtCollector;

    @Autowired
    SeoulLtRsdtCollector ltRsdtCollector;

    @Autowired
    ExternalMyHomeProperties props;

    @Autowired
    MyHomeApiClient client;

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Test
    void rsdt_latest_in_last_12_months_and_seoul_filter_and_fields() {
        // 최근 12개월에서  역순으로 RSDT 데이터를 찾는다
        YearMonth now = YearMonth.now(SEOUL);

        FoundResult found = null;
        for (int i = 0; i < 12; i++) {
            YearMonth ym = now.minusMonths(i);
            List<MyHomeItemDto> items = rsdtCollector.collect(baseRsdtRequestFor(ym));
            if (items != null && !items.isEmpty()) {
                found = new FoundResult(ym, items);
                break;
            }
        }

        // 최근 12개월에 데이터가 아예 없으면  실패시키되 메시지로 원인 파악 가능하게끔 해주기
        assertThat(found)
                .withFailMessage("RSDT: 최근 12개월 내 서울 데이터가 하나도 없음(진짜 공고가 없거나 API 상태/파라미터 문제)")
                .isNotNull();

        // 찾은 월의 데이터가 서울인지 확인하기
        assertAllItemsLookLikeSeoul(found.items);

        // 최소 필드가 알맞게 들어오는지 확인하기
        assertBasicFieldsPresent(found.items);
    }

    @Test
    void ltrsdt_latest_in_last_12_months_and_seoul_filter_and_fields_or_empty_ok() {
        // 최근 12개월에서  역순으로 LTRSDT 데이터를 찾는다 만약 없다?? 그럼 통과하기
        YearMonth now = YearMonth.now(SEOUL);

        FoundResult found = null;
        for (int i = 0; i < 12; i++) {
            YearMonth ym = now.minusMonths(i);
            List<MyHomeItemDto> items = ltRsdtCollector.collect(baseLtRsdtRequestFor(ym));
            if (items != null && !items.isEmpty()) {
                found = new FoundResult(ym, items);
                break;
            }
        }

        // LTRSDT는 실제로 없을 수 있어서 있으면 검증 없으면 스모크로 ok
        if (found == null) {
            return;
        }

        assertAllItemsLookLikeSeoul(found.items);
        assertBasicFieldsPresent(found.items);
    }

    @Test
    void log_should_mask_service_key_and_keep_seoul_param_in_uri(CapturedOutput output) {
        // 일부러 데이터 없을 확률이 매우 높은 미래 월로 호출해서 no data 로그를 발생시키기
        YearMonth future = YearMonth.now(SEOUL).plusMonths(24);

        RsdtListRequest req = RsdtListRequest.builder()
                .pageNo(1)
                .numOfRows(10)
                .brtcCode("11") // 서울
                .yearMtBegin(yyyyMM(future))
                .yearMtEnd(yyyyMM(future))
                .build();

        client.fetchRsdt(req); // no data면 MyHomeApiClient가 info 로그를 찍는다

        String out = output.getOut();
        if (out == null || out.isBlank()) return;

        // serviceKey는 마스킹되는지 체크하기
        assertThat(out).contains("serviceKey=****");

        // 실제 serviceKey 값은 로그에 절대 나오면 안되니까 테스트해보기
        assertThat(out).doesNotContain(props.getServiceKey());

        // 서울 필터가 실제 URI에 포함되어 나갔는지 확인
        assertThat(out).contains("brtcCode=11");
    }


    // helpers


    // 해당 월로 기본 RSDT 요청 생성
    private RsdtListRequest baseRsdtRequestFor(YearMonth ym) {
        return RsdtListRequest.builder()
                .pageNo(1)
                .numOfRows(200)
                .yearMtBegin(yyyyMM(ym))
                .yearMtEnd(yyyyMM(ym))
                .build();
    }

    // 해당 월로 기본 LTRSDT 요청 생성
    private LtRsdtListRequest baseLtRsdtRequestFor(YearMonth ym) {
        return LtRsdtListRequest.builder()
                .pageNo(1)
                .numOfRows(200)
                .yearMtBegin(yyyyMM(ym))
                .yearMtEnd(yyyyMM(ym))
                .build();
    }

    // 결과 item들이 서울로 보이는지 확인하기
    private void assertAllItemsLookLikeSeoul(List<MyHomeItemDto> items) {
        for (MyHomeItemDto it : items) {
            // brtcNm이 서울을 포함하는지 확인하기
            if (it.getBrtcNm() != null && !it.getBrtcNm().isBlank()) {
                assertThat(it.getBrtcNm())
                        .withFailMessage("서울 필터인데 brtcNm이 서울이 아님: brtcNm=%s, pblancId=%s",
                                it.getBrtcNm(), it.getPblancId())
                        .contains("서울");
            }
        }
    }

    // 정보가 잘들어오는지 최소필드로 검증하기
    private void assertBasicFieldsPresent(List<MyHomeItemDto> items) {
        for (MyHomeItemDto it : items) {
            assertThat(it.getPblancId())
                    .withFailMessage("pblancId(공고ID) 누락")
                    .isNotBlank();
            assertThat(it.getPblancNm())
                    .withFailMessage("pblancNm(공고명) 누락. pblancId=%s", it.getPblancId())
                    .isNotBlank();
            assertThat(it.getSuplyInsttNm())
                    .withFailMessage("suplyInsttNm(공급기관명) 누락. pblancId=%s", it.getPblancId())
                    .isNotBlank();

            // 날짜/링크/주소는 공고마다 비어있을 수 있어서 강제하지 않기
        }
    }

    private static String yyyyMM(YearMonth ym) {
        int y = ym.getYear();
        int m = ym.getMonthValue();
        return y + (m < 10 ? "0" + m : String.valueOf(m));
    }

    private record FoundResult(YearMonth ym, List<MyHomeItemDto> items) {}
}
