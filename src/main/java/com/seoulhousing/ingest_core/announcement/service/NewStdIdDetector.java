package com.seoulhousing.ingest_core.announcement.service;


import java.util.List;

public interface NewStdIdDetector {

    // 수집한 stdId들 중에서 신규 stdId만 골라내는 서비스
    List<String> detectNewStdIds(String source, String category, String scope, List<String> currentStdIds);
}
