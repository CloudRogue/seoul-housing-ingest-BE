## 1. 담당자

- 담당자: 신수호
- 담당 영역:
  - 서울 주택 공고 수집 서버 설계 및 구현
  - MyHome 공공임대/공공분양 공고 수집 기능 구현
  - SH RSS 공고 수집 및 신규 공고 선별 기능 구현
  - Redis 기반 공고 변경 감지 기능 구현
  - 수집된 공고 데이터를 메인 서버 ingest API로 전송하는 연동 기능 구현

<br><br>

## 공고 수집

<br>

### MyHome 공고

MyHome 공고는 공공임대와 공공분양 카테고리로 나누어 수집.

1. 설정된 수집 개수(`numOfRows`)를 기준으로 MyHome API 요청 생성
2. 공공임대 공고 목록 수집
3. 공공분양 공고 목록 수집
4. 각 공고의 `pblancId`, `houseSn` 값을 기반으로 표준 ID 생성
5. Redis에 저장된 기존 공고 ID 목록과 비교
6. 신규 공고만 메인 서버 전송 DTO로 변환
7. 메인 서버 ingest API 호출

<br>

### SH RSS 공고

SH 공고는 RSS 데이터를 기반으로 수집.

1. SH RSS 원문 데이터 요청
2. XML 데이터를 공고 아이템 목록으로 파싱
3. 발행일과 `seq` 기준으로 최신순 정렬
4. 각 RSS 공고의 `seq`를 기반으로 표준 ID 생성
5. Redis에 저장된 기존 공고 ID 목록과 비교
6. 최초 실행 시에는 설정된 `seedLimit` 개수만 전송
7. 이후 실행부터는 신규 공고만 메인 서버 전송 DTO로 변환
8. 메인 서버 ingest API 호출

<br><br>

## 공고 변경 감지

<br>

### Redis 기반 신규/누락 공고 감지

이 서버는 수집된 공고를 바로 모두 전송하지 않고, Redis에 저장된 기존 공고 ID와 현재 수집 결과를 비교하여 신규 공고와 누락 공고를 판단합니다.

<br>

### 처리 방식

1. `source`, `category`, `scope` 기준으로 Redis key 생성
2. Redis Set에서 기존에 확인한 표준 공고 ID 목록 조회
3. 현재 수집된 공고 ID 목록과 비교
4. 기존에 없던 ID는 신규 공고로 판단
5. 기존에는 있었지만 현재 수집 결과에 없는 ID는 누락 공고로 판단
6. 신규 공고만 메인 서버 전송 대상으로 선별

<br>

### Redis key 구조

```text
seoulhousing:{env}:seen:{source}:{category}:{scope}
```

예시:

```text
seoulhousing:prod:seen:myhome:rsdt:default
seoulhousing:prod:seen:sh:rental:default
```

<br><br>

## 메인 서버 공고 전송

<br>

### MainServerIngestClient

수집 서버는 신규 공고를 자체 DB에 저장하지 않고, 메인 서버의 ingest API로 전달합니다.

<br>

### 전송 요청 구조

```json
{
  "category": "rsdt",
  "items": [
    {
      "source": "MYHOME",
      "externalKey": "12345:1",
      "title": "청년 매입임대주택 입주자 모집공고",
      "publisher": "LH",
      "housingType": "매입임대",
      "supplyType": "공공임대",
      "regionName": "서울특별시",
      "startDate": "2026-01-01",
      "endDate": "2026-01-10",
      "documentPublishedAt": null,
      "finalPublishedAt": "2026-01-20",
      "applyUrl": "https://example.com",
      "rentGtn": 10000000,
      "enty": null,
      "prtpay": null,
      "surlus": null,
      "mtRntchrg": 300000,
      "fullAddress": "서울특별시 ...",
      "refrnLegaldongNm": "서울 ..."
    }
  ]
}
```

<br>

### 전송 응답 구조

```json
{
  "received": 10,
  "created": 8,
  "updated": 1,
  "skipped": 1
}
```

<br>

### 전송 흐름

```text
공고 수집
  ↓
표준 ID 생성
  ↓
Redis 기준 신규 공고 감지
  ↓
신규 공고만 AnnouncementIngestItem으로 변환
  ↓
AnnouncementIngestRequest 생성
  ↓
메인 서버 ingest API 호출
  ↓
received / created / updated / skipped 결과 로그 기록
```

<br><br>

## 수집 잡 실행 흐름

<br>

### IngestJobService

수집 잡은 `runOnce()` 단위로 실행됩니다.

```text
runOnce()
  ↓
MyHome 공공임대 수집
  ↓
MyHome 공공분양 수집
  ↓
SH RSS 수집
  ↓
각 수집 결과에 대해 신규 공고 감지
  ↓
신규 공고가 있으면 메인 서버 ingest API 호출
  ↓
신규 공고가 없으면 전송 스킵
```

<br><br>

## 주요 처리 로직

<br>

### MyHome 수집 처리

1. `RsdtListRequest`로 공공임대 목록 요청
2. `LtRsdtListRequest`로 공공분양 목록 요청
3. 각 공고에서 `pblancId`, `houseSn` 추출
4. 카테고리와 식별자를 조합해 표준 ID 생성
5. Redis 기준 신규 여부 판단
6. 신규 공고만 `AnnouncementIngestItem`으로 매핑
7. 메인 서버로 전송

<br>

### SH RSS 수집 처리

1. SH RSS 전체 아이템 수집
2. 발행일과 `seq` 기준 최신순 정렬
3. `seq` 기반 표준 ID 생성
4. Redis 기준 신규 여부 판단
5. 최초 실행이면 `seedLimit`만큼만 전송
6. 이후 실행이면 신규 공고만 전송
7. SH RSS 데이터는 부족한 필드가 많으므로 제목, 게시자, 링크 중심으로 매핑

<br>

### 메인 서버 연동 처리

1. `main-server.base-url`과 `main-server.ingest-path` 설정 사용
2. `RestClient`로 JSON POST 요청 전송
3. 실패 시 retry executor를 통해 재시도
4. HTTP 실패 응답은 status와 body를 로그로 기록
5. 정상 응답이면 생성/수정/스킵 결과를 로그로 기록

<br><br>

## 기술 스택

| 구분 | 사용 기술 |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.1 |
| Web | Spring Web MVC |
| Validation | Spring Validation, Jakarta Validation |
| Cache / State Store | Redis |
| HTTP Client | Spring RestClient |
| Retry | Spring Retry, Spring AOP |
| Build Tool | Gradle |
| Container | Docker |
| Runtime Image | Amazon Corretto 21 |

<br><br>

## 패키지 구조

```text
src/main/java/com/seoulhousing/ingest_core/
  IngestCoreApplication.java

  announcement/
    adapter/
      RedisSeenStdIdReaderAdapter.java
    dto/
      ChangeDetectionResult.java
    port/
      SeenStdIdReaderPort.java
    service/
      AnnouncementChangeDetectionService.java
      DefaultAnnouncementChangeDetectionService.java
      NewStdIdDetector.java
      MissingStdIdDetector.java
      StdIdGenerator.java

  external/
    myhome/
      dto/
      service/
    sh/
      client/
      dto/
      service/

  mainserver/
    client/
      MainServerIngestClient.java
      MainServerRetryExecutor.java
    dto/
      AnnouncementIngestRequest.java
      AnnouncementIngestItem.java
      IngestResponse.java
      MainServerAnnouncementSource.java
    mapper/
      MyHomeToIngestItemMapper.java
      ShRssToIngestItemMapper.java
    service/
      IngestJobService.java
      DefaultIngestJobService.java
      ShRssIngestService.java
      DefaultShRssIngestService.java

src/main/resources/
  application.yml

build.gradle
settings.gradle
Dockerfile
README.md
```
