package com.seoulhousing.ingest_core.announcement.service;

import java.util.List;

//누락후보
public interface MissingStdIdDetector {
    List<String> detectMissingStdIds(String source, String category, String scope, List<String> currentStdIds);
}
