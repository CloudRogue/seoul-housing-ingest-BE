package com.seoulhousing.ingest_core.external.sh.service;

import com.seoulhousing.ingest_core.external.sh.client.ShRssApiClient;
import com.seoulhousing.ingest_core.external.sh.dto.ShRssItem;
import com.seoulhousing.ingest_core.mainserver.dto.AnnouncementIngestItem;
import com.seoulhousing.ingest_core.mainserver.service.ShRssIngestService;
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
    ShRentalNoticeCheckerService checkerService; // fetchAllItems()

    @Autowired
    ShRssIngestService shRssIngestService; // collectNewItems()는 이 서비스가 담당

    @Test
    void fetch_notice_rss_bytes_should_not_be_empty_and_looks_like_xml() {
        byte[] bytes = client.fetchNoticeRssBytes();

        assertThat(bytes).isNotNull();
        assertThat(bytes.length).isGreaterThan(50);

        // XML 헤더/루트 태그 같은 "ASCII 영역"은 EUC-KR이어도 그대로 보임
        String head = new String(bytes, 0, Math.min(bytes.length, 200), StandardCharsets.ISO_8859_1);

        assertThat(head).contains("<?xml");
        assertThat(head).contains("<rss");
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

        long hasSeqCount = items.stream()
                .filter(it -> it.getSeq() != null && !it.getSeq().isBlank())
                .count();

        assertThat(hasSeqCount)
                .withFailMessage("SH RSS item 중 seq가 하나도 추출 안됨(링크 포맷 변경 or seq extractor 문제)")
                .isGreaterThan(0);

        long hasPublishedAtCount = items.stream()
                .filter(it -> it.getPublishedAt() != null)
                .count();

        assertThat(hasPublishedAtCount)
                .withFailMessage("SH RSS item의 publishedAt이 전부 null이면 pubDate 포맷 변경/파서 문제 가능성")
                .isGreaterThan(0);
    }

    @Test
    void checker_service_should_fetch_items_and_log(CapturedOutput output) {
        List<ShRssItem> items = checkerService.fetchAllItems();

        assertThat(items).isNotNull();
        assertThat(items).isNotEmpty();

        // 로그는 환경에 따라 달라서 약하게만 체크
        String out = output.getOut();
        if (out != null && !out.isBlank()) {
            assertThat(out).contains("[SH][RSS]");
        }
    }

    @Test
    void ingest_service_should_return_items_only_when_new_exists_or_first_run_seed_applies() {
        // DefaultShRssIngestService.collectNewItems(category) 검증
        // 이 메서드는 "seenCount=0(최초실행)"이면 seedLimit만큼 리턴할 수도 있고,
        // seen이 있으면 실제 신규만 리턴함.
        // 데이터/레디스 상태에 따라 결과가 달라질 수 있으니 "안 터지게" 스모크로만 검증.

        String category = "rental";

        List<AnnouncementIngestItem> items = shRssIngestService.collectNewItems(category);

        assertThat(items).isNotNull();
        // 0개여도 정상(신규 없으면 0개 반환하는 로직이니까)
        // 다만 있으면 externalKey는 반드시 있어야 함
        items.forEach(it -> assertThat(it.externalKey()).isNotBlank());
    }

    // (옵션) 최신 seq 뽑는 헬퍼: 필요하면 유지
    private static String pickLatestSeqOrNull(List<ShRssItem> items) {
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
            return String.valueOf(sb).compareTo(String.valueOf(sa));
        }
    }
}
