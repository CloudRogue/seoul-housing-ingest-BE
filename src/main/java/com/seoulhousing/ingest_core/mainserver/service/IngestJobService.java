package com.seoulhousing.ingest_core.mainserver.service;

//파싱서버가 켜지면 1번 실행할 작업의 인터페이스
public interface IngestJobService {

    //수집 -> 변경감지 -> 신규발견 -> 메인서버 ingest 호출(한번 실행)
    void runOnce();
}
