package com.seoulhousing.ingest_core.announcement.service;


import com.seoulhousing.ingest_core.announcement.dto.ChangeDetectionResult;

import java.util.List;


//공고 변경 감지 서비스
public interface AnnouncementChangeDetectionService {

    ChangeDetectionResult detect(
            String source,
            String category,
            String scope,
            List<String> currentStdIds
    );
}