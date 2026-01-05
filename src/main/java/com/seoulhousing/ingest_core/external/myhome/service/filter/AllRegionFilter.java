package com.seoulhousing.ingest_core.external.myhome.service.filter;

import com.seoulhousing.ingest_core.external.myhome.dto.MyHomeItemDto;

// 필터 없음(전국)
public class AllRegionFilter implements RegionFilter{
    @Override
    public boolean matches(MyHomeItemDto item) {
        return true;
    }
}
