package com.seoulhousing.ingest_core.redis;

import org.jspecify.annotations.Nullable;

import java.util.Map;

public interface ShRssSnapshotReaderPort {

    Map<String, String> getMeta(String category, String scope);


    // 원문 그대로 저장
    @Nullable
    byte[] getSnapshotBytes(String category, String scope);
}
