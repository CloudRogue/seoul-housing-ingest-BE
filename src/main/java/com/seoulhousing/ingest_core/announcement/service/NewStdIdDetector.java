package com.seoulhousing.ingest_core.announcement.service;


import java.util.List;
import java.util.Set;

public interface NewStdIdDetector {

    // 수집한 stdId들 중에서 신규 stdId만 골라내는 서비스
    List<String> detect(Set<String> seenStdIds, List<String> currentStdIds);
}
