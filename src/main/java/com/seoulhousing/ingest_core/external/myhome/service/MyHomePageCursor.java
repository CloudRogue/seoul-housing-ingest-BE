package com.seoulhousing.ingest_core.external.myhome.service;

import com.seoulhousing.ingest_core.external.myhome.dto.MyHomeItemDto;
import com.seoulhousing.ingest_core.external.myhome.dto.MyHomeListResponse;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.IntFunction;

// 페이지 전용 객체
public final class MyHomePageCursor {
    private int pageNo = 1;
    private boolean finished = false;

    private final IntFunction<MyHomeListResponse> fetchByPageNo;

    public MyHomePageCursor(IntFunction<MyHomeListResponse> fetchByPageNo) {
        this.fetchByPageNo = Objects.requireNonNull(fetchByPageNo);
    }

    public Optional<List<MyHomeItemDto>> next() {
        if (finished) return Optional.empty();

        MyHomeListResponse res = fetchByPageNo.apply(pageNo);
        List<MyHomeItemDto> items = res.itemsOrEmpty();

        if (items.isEmpty()) {
            finished = true;
            return Optional.empty();
        }

        pageNo++;
        return Optional.of(items);
    }
}
