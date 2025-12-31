package com.seoulhousing.ingest_core.external.myhome.client;

import com.seoulhousing.ingest_core.external.myhome.dto.MyHomeListRequest;
import com.seoulhousing.ingest_core.external.myhome.dto.MyHomeListResponse;
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
        // 최소 파라미터만 (pageNo/numOfRows) 넣어서 호출
        MyHomeListRequest req = MyHomeListRequest.builder()
                .pageNo(1)
                .numOfRows(10)
                .build();

        MyHomeListResponse res = myHomeApiClient.fetchList(MyHomeApiClient.Category.RSDT, req);

        //검증
        assertThat(res).isNotNull();
        assertThat(res.getResponse()).isNotNull();
        assertThat(res.getResponse().getHeader()).isNotNull();
        assertThat(res.getResponse().getHeader().getResultCode()).isEqualTo("00");


        var body = res.getResponse().getBody();
        String totalCount = (body == null) ? "null" : body.getTotalCount();
        List<?> items = (body == null || body.getItem() == null) ? List.of() : body.getItem();

        System.out.println("[RSDT] totalCount=" + totalCount);
        System.out.println("[RSDT] items.size=" + items.size());


    }

    @Test
    void fetch_ltrsdT_list_print_summary() {
        MyHomeListRequest req = MyHomeListRequest.builder()
                .pageNo(1)
                .numOfRows(10)
                .build();

        MyHomeListResponse res = myHomeApiClient.fetchList(MyHomeApiClient.Category.LTRSDT, req);

        assertThat(res).isNotNull();
        assertThat(res.getResponse()).isNotNull();
        assertThat(res.getResponse().getHeader()).isNotNull();
        assertThat(res.getResponse().getHeader().getResultCode()).isEqualTo("00");

        var body = res.getResponse().getBody();
        String totalCount = (body == null) ? "null" : body.getTotalCount();
        List<?> items = (body == null || body.getItem() == null) ? List.of() : body.getItem();

        System.out.println("[LTRSDT] totalCount=" + totalCount);
        System.out.println("[LTRSDT] items.size=" + items.size());
    }

    @Test
    void fetchList_should_throw_when_request_is_null() {
        assertThatThrownBy(() -> myHomeApiClient.fetchList(MyHomeApiClient.Category.RSDT, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("request must not be null");
    }

    @Test
    void fetchList_should_throw_when_category_is_null() {
        MyHomeListRequest req = MyHomeListRequest.builder()
                .pageNo(1)
                .numOfRows(10)
                .build();

        assertThatThrownBy(() -> myHomeApiClient.fetchList(null, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("category must not be null");
    }
}
