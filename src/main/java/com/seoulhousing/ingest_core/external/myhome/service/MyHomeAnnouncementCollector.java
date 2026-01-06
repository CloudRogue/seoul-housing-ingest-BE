package com.seoulhousing.ingest_core.external.myhome.service;

import com.seoulhousing.ingest_core.external.myhome.dto.LtRsdtListRequest;
import com.seoulhousing.ingest_core.external.myhome.dto.MyHomeItemDto;
import com.seoulhousing.ingest_core.external.myhome.dto.RsdtListRequest;

import java.util.List;

public interface MyHomeAnnouncementCollector {

    List<MyHomeItemDto> collectRsdt(RsdtListRequest request);

    List<MyHomeItemDto> collectLtRsdt(LtRsdtListRequest request);
}
