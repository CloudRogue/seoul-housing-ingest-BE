package com.seoulhousing.ingest_core.redis;



import org.jspecify.annotations.Nullable;

import java.util.Map;

public interface MyHomeSnapshotReaderPort {

    // 메타의 해쉬값
    Map<String,String> getMeta(String category, String scope);

    // 체크섬의 해쉬
    @Nullable
    String getChecksum(String category, String scope, String stdId);

    //체크섬 전체 가져오기
    Map<String,String> getAllChecksums(String category, String scope);

    //스냅샷 페이로드
    @Nullable
    byte[] getSnapshotJsonBytes(String category, String scope);
}
