package com.seoulhousing.ingest_core.external.myhome.service.filter;

import com.seoulhousing.ingest_core.external.myhome.dto.MyHomeItemDto;

public interface RegionFilter {

    // 지역 판정 로직
    boolean matches(MyHomeItemDto item);
}
