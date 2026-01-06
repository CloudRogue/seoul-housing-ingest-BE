package com.seoulhousing.ingest_core.external.myhome.service;

import com.seoulhousing.ingest_core.external.myhome.dto.LtRsdtListRequest;
import com.seoulhousing.ingest_core.external.myhome.dto.MyHomeItemDto;


import java.util.List;

public interface LtRsdtCollector {

    List<MyHomeItemDto> collect(LtRsdtListRequest request);
}
