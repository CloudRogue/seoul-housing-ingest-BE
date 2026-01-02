package com.seoulhousing.ingest_core.external.myhome.client;

import com.seoulhousing.ingest_core.external.myhome.dto.LtRsdtListRequest;
import com.seoulhousing.ingest_core.external.myhome.dto.MyHomeListResponse;
import com.seoulhousing.ingest_core.external.myhome.dto.RsdtListRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class MyHomeApiClientTest {

    @Autowired
    private MyHomeApiClient myHomeApiClient;

    @Test
    void fetch_rsdt_list_print_summary() {
        // given
        RsdtListRequest req = RsdtListRequest.builder()
                .pageNo(1)
                .numOfRows(10)
                .build();

        // when
        MyHomeListResponse res = myHomeApiClient.fetchRsdt(req);

        // then
        assertThat(res).isNotNull();
        assertThat(res.getResponse()).isNotNull();
        assertThat(res.getResponse().getHeader()).isNotNull();
        assertThat(res.getResponse().getHeader().getResultCode()).isEqualTo("00");

        // summary 출력
        var body = res.getResponse().getBody();
        String totalCount = (body == null) ? "null" : body.getTotalCount();
        List<?> items = (body == null || body.getItem() == null) ? List.of() : body.getItem();

        System.out.println("[RSDT] totalCount=" + totalCount);
        System.out.println("[RSDT] items.size=" + items.size());
    }

    @Test
    void fetch_ltrsdT_list_print_summary() {
        // given
        LtRsdtListRequest req = LtRsdtListRequest.builder()
                .pageNo(1)
                .numOfRows(10)
                .build();

        // when
        MyHomeListResponse res = myHomeApiClient.fetchLtRsdt(req);

        // then
        assertThat(res).isNotNull();
        assertThat(res.getResponse()).isNotNull();
        assertThat(res.getResponse().getHeader()).isNotNull();
        assertThat(res.getResponse().getHeader().getResultCode()).isEqualTo("00");

        // summary 출력
        var body = res.getResponse().getBody();
        String totalCount = (body == null) ? "null" : body.getTotalCount();
        List<?> items = (body == null || body.getItem() == null) ? List.of() : body.getItem();

        System.out.println("[LTRSDT] totalCount=" + totalCount);
        System.out.println("[LTRSDT] items.size=" + items.size());
    }

    @Test
    void fetchRsdt_should_throw_when_request_is_null() {
        assertThatThrownBy(() -> myHomeApiClient.fetchRsdt(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("request must not be null");
    }

    @Test
    void fetchLtRsdt_should_throw_when_request_is_null() {
        assertThatThrownBy(() -> myHomeApiClient.fetchLtRsdt(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("request must not be null");
    }
}
