package com.seoulhousing.ingest_core.external.sh.service;

import com.seoulhousing.ingest_core.config.ExternalShRssProperties;
import com.seoulhousing.ingest_core.external.sh.client.ShRssApiClient;
import com.seoulhousing.ingest_core.external.sh.dto.ShRssDiffResult;
import com.seoulhousing.ingest_core.external.sh.dto.ShRssItem;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("it")
class ShRssLocalSmokeIT {

    @Autowired
    ShRssApiClient client;

    @Autowired
    ShRssXmlParser parser;

    @Autowired
    ShRssRentalNoticeDiff diff;

    @Autowired
    ShRentalNoticeCheckerService checkerService;

    @Autowired
    ExternalShRssProperties props;

    @Test
    void fetch_notice_rss_bytes_should_not_be_empty_and_looks_like_xml() {
        byte[] bytes = client.fetchNoticeRssBytes();

        assertThat(bytes).isNotNull();
        assertThat(bytes.length).isGreaterThan(50);

        // XML 헤더/루트 태그 같은 "ASCII 영역"은 EUC-KR이어도 그대로 보임
        String head = new String(bytes, 0, Math.min(bytes.length, 200), StandardCharsets.ISO_8859_1);

        assertThat(head).contains("<?xml");
        assertThat(head).contains("<rss");
        // 보통 EUC-KR이지만, 혹시 바뀌더라도 깨지지 않게 강제는 약하게
        assertThat(head).contains("encoding=");
    }

    @Test
    void parse_should_return_items_and_seq_should_exist_for_some_items() {
        byte[] bytes = client.fetchNoticeRssBytes();

        List<ShRssItem> items = parser.parse(bytes);

        assertThat(items).isNotNull();
        assertThat(items.size())
                .withFailMessage("SH RSS 파싱 결과 item이 0개면 RSS 상태/파서 문제 가능성 큼")
                .isGreaterThan(0);

        // 최소한 seq는 추출 가능한 아이템이 있어야 함 (link에 seq 쿼리 있음)
        long hasSeqCount = items.stream()
                .filter(it -> it.getSeq() != null && !it.getSeq().isBlank())
                .count();

        assertThat(hasSeqCount)
                .withFailMessage("SH RSS item 중 seq가 하나도 추출 안됨(링크 포맷 변경 or seq extractor 문제)")
                .isGreaterThan(0);

        // pubDate는 RSS마다 null일 수 있으니 "전부 null은 아닌지" 정도만
        long hasPublishedAtCount = items.stream()
                .filter(it -> it.getPublishedAt() != null)
                .count();

        assertThat(hasPublishedAtCount)
                .withFailMessage("SH RSS item의 publishedAt이 전부 null이면 pubDate 포맷 변경/파서 문제 가능성")
                .isGreaterThan(0);
    }

    @Test
    void diff_should_find_lastSeenSeq_when_using_latestSeq_and_hasNewRental_should_be_false() {
        byte[] bytes = client.fetchNoticeRssBytes();
        List<ShRssItem> items = parser.parse(bytes);

        // 최신 seq를 lastSeenSeq로 넣으면 "신규 없음"이 정상 시나리오
        String latestSeq = pickLatestSeqOrNull(items);
        assertThat(latestSeq)
                .withFailMessage("latestSeq를 못 뽑으면 정렬/seq 추출 이상 가능성")
                .isNotBlank();

        ShRssDiffResult result = diff.diff(items, latestSeq);

        assertThat(result.isLastSeenFound())
                .withFailMessage("latestSeq를 lastSeenSeq로 넣었는데 못 찾으면 정렬/비교 로직 의심")
                .isTrue();

        assertThat(result.isHasNewRental())
                .withFailMessage("latestSeq를 기준으로 신규 임대가 있다고 나오면 diff 로직이 이상할 수 있음")
                .isFalse();

        assertThat(result.getLatestSeq()).isNotBlank();
    }

    @Test
    void checker_service_should_log_debug_and_return_result(CapturedOutput output) {
        // 서비스 단에서 end-to-end로 한 번
        byte[] bytes = client.fetchNoticeRssBytes();
        List<ShRssItem> items = parser.parse(bytes);
        String latestSeq = pickLatestSeqOrNull(items);
        assertThat(latestSeq).isNotBlank();

        ShRssDiffResult result = checkerService.checkNewRentalNotice(latestSeq);

        assertThat(result).isNotNull();
        assertThat(result.isLastSeenFound()).isTrue();
        assertThat(result.getLatestSeq()).isNotBlank();

        // debug 로그는 환경(로깅 레벨 설정)에 따라 안 찍힐 수 있어서
        // "있으면 확인" 정도로만 약하게 체크 (강제하면 CI/환경마다 깨짐)
        String out = output.getOut();
        if (out != null && !out.isBlank()) {
            assertThat(out).contains("[SH][RSS]");
        }
    }

    private static String pickLatestSeqOrNull(List<ShRssItem> items) {
        // ShRssRentalNoticeDiff와 최대한 동일한 기준(가능하면 publishedAt desc)
        return items.stream()
                .filter(it -> it.getSeq() != null && !it.getSeq().isBlank())
                .sorted((a, b) -> {
                    Instant pa = a.getPublishedAt();
                    Instant pb = b.getPublishedAt();
                    if (pa == null && pb == null) return compareSeqDesc(a.getSeq(), b.getSeq());
                    if (pa == null) return 1;
                    if (pb == null) return -1;
                    int c = pb.compareTo(pa);
                    if (c != 0) return c;
                    return compareSeqDesc(a.getSeq(), b.getSeq());
                })
                .map(ShRssItem::getSeq)
                .findFirst()
                .orElse(null);
    }

    private static int compareSeqDesc(String sa, String sb) {
        try {
            long a = Long.parseLong(sa);
            long b = Long.parseLong(sb);
            return Long.compare(b, a);
        } catch (Exception e) {
            // 숫자 파싱 실패하면 문자열 기준
            return String.valueOf(sb).compareTo(String.valueOf(sa));
        }
    }

    @Test
    void diff_should_collect_only_rental_items_between_latest_and_lastSeen() {
        byte[] bytes = client.fetchNoticeRssBytes();
        List<ShRssItem> items = parser.parse(bytes);
        assertThat(items).isNotEmpty();

        // 최신순 기준을 diff와 동일하게 맞추자(테스트용 정렬)
        List<ShRssItem> sorted = items.stream()
                .filter(it -> it.getSeq() != null && !it.getSeq().isBlank())
                .sorted((a,b) -> {
                    Instant pa = a.getPublishedAt();
                    Instant pb = b.getPublishedAt();
                    if (pa == null && pb == null) return compareSeqDesc(a.getSeq(), b.getSeq());
                    if (pa == null) return 1;
                    if (pb == null) return -1;
                    int c = pb.compareTo(pa);
                    if (c != 0) return c;
                    return compareSeqDesc(a.getSeq(), b.getSeq());
                })
                .toList();

        // lastSeen을 "조금 과거"로 잡아야 신규 구간이 생김 (예: 10번째)
        Assumptions.assumeTrue(sorted.size() > 10, "아이템이 너무 적으면 스킵");
        String lastSeenSeq = sorted.get(10).getSeq();

        ShRssDiffResult result = diff.diff(items, lastSeenSeq);

        // lastSeen은 반드시 찾아야 정상
        assertThat(result.isLastSeenFound()).isTrue();
        assertThat(result.getLatestSeq()).isNotBlank();

        // ✅ 핵심: newRentalItems는 전부 '임대' 포함이어야 함
        assertThat(result.getNewRentalItems())
                .allSatisfy(it -> assertThat(it.getTitle()).contains("임대"));

        // 실제 신규 구간(0~9)에서 임대가 하나라도 있으면 hasNewRental은 true여야 함
        boolean expectedHasNewRental = sorted.subList(0, 10).stream()
                .map(ShRssItem::getTitle)
                .filter(t -> t != null && !t.isBlank())
                .anyMatch(t -> t.contains("임대"));

        // ✅ 데이터에 따라 임대가 없을 수도 있으니 기대값 비교로 안전하게
        assertThat(result.isHasNewRental()).isEqualTo(expectedHasNewRental);

        // 사람이 눈으로도 확인하고 싶으면 상위 몇개 출력
        System.out.println("lastSeenSeq=" + lastSeenSeq + ", latestSeq=" + result.getLatestSeq());
        result.getNewRentalItems().stream().limit(5).forEach(it ->
                System.out.println("[NEW RENTAL] " + it.getSeq() + " | " + it.getTitle())
        );
    }


}
