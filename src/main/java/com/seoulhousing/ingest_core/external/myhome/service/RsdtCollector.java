package com.seoulhousing.ingest_core.external.myhome.service;

import com.seoulhousing.ingest_core.external.myhome.dto.MyHomeItemDto;
import com.seoulhousing.ingest_core.external.myhome.dto.RsdtListRequest;

import java.util.List;

public interface RsdtCollector {

    List<MyHomeItemDto> collect(RsdtListRequest request);
}
