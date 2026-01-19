package com.seoulhousing.ingest_core.announcement.service;

import java.util.List;
import java.util.Set;

//누락후보
public interface MissingStdIdDetector {
    List<String> detect(Set<String> seenStdIds, List<String> currentStdIds);
}
