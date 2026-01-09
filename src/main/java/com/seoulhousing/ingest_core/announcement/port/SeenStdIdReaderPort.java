package com.seoulhousing.ingest_core.announcement.port;

import java.util.Set;

// Redis에서 이미 존재하는 StdId를 조회할수 있도록해주는 포트
public interface SeenStdIdReaderPort {

    Set<String> getSeenStdIds(String source, String category, String scope);

}
