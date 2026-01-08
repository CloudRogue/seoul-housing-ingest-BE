package com.seoulhousing.ingest_core.external.sh.service;

import com.seoulhousing.ingest_core.external.sh.dto.ShRssItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * ShRssXmlParser 단위 테스트 (StAX 버전)
 *
 * 목표:
 * - RSS 원문(byte[]) -> ShRssItem 리스트 변환이 정상 동작하는지 확인
 * - title/link 누락, seq 누락 등 "비교 불가능"한 항목은 스킵하는 정책 고정
 * - EUC-KR 인코딩 + 한글 포함 상황에서도 파싱이 깨지지 않는지 확인
 * - pubDate(RFC1123) 파싱이 Instant로 변환되는지 확인
 */
class ShRssXmlParserTest {

    private final ShRssXmlParser parser = new ShRssXmlParser(new ShRssSeqExtractor());

    @Test
    @DisplayName("정상 RSS: item 2개를 파싱하고 seq/title/link/publishedAt을 채운다")
    void parse_shouldReturnItems_whenValidRss() {
        // given
        String xml = rssXml(
                itemXml("298537", "행복주택 예비자 계약결과 알림", "http://www.i-sh.co.kr/view.do?seq=298537",
                        "Mon, 05 Jan 2026 06:48:24 GMT"),
                itemXml("298522", "[임대] 청년안심주택 예비2차 입주안내", "http://www.i-sh.co.kr/view.do?seq=298522",
                        "Mon, 05 Jan 2026 05:02:29 GMT")
        );
        byte[] rssBytes = xml.getBytes(Charset.forName("EUC-KR"));

        // when
        List<ShRssItem> items = parser.parse(rssBytes);

        // then
        assertThat(items).hasSize(2);

        assertThat(items.get(0).getSeq()).isEqualTo("298537");
        assertThat(items.get(0).getTitle()).isEqualTo("행복주택 예비자 계약결과 알림");
        assertThat(items.get(0).getLink()).isEqualTo("http://www.i-sh.co.kr/view.do?seq=298537");
        assertThat(items.get(0).getPublishedAt()).isNotNull();

        assertThat(items.get(1).getSeq()).isEqualTo("298522");
        assertThat(items.get(1).getTitle()).isEqualTo("[임대] 청년안심주택 예비2차 입주안내");
        assertThat(items.get(1).getLink()).isEqualTo("http://www.i-sh.co.kr/view.do?seq=298522");
        assertThat(items.get(1).getPublishedAt()).isNotNull();

        // publishedAt이 내림차순으로 들어오는지까지는 parser 책임이 아니지만,
        // 최소한 Instant 파싱이 되는지만 확인할 수도 있음(옵션)
        Instant t0 = items.get(0).getPublishedAt();
        Instant t1 = items.get(1).getPublishedAt();
        assertThat(t0).isAfter(t1);
    }

    @Test
    @DisplayName("title이 없거나 link가 없으면 스킵한다")
    void parse_shouldSkip_whenTitleOrLinkMissing() {
        // given: 3개 중 1개만 정상
        String xml = rssXml(
                // title 없음 -> 스킵
                "<item><link>http://www.i-sh.co.kr/view.do?seq=298111</link></item>",
                // link 없음 -> 스킵
                "<item><title>임대 공고</title></item>",
                // 정상
                itemXml("298120", "정상 공고", "http://www.i-sh.co.kr/view.do?seq=298120",
                        "Mon, 06 Jan 2026 12:00:00 +0900")
        );
        byte[] rssBytes = xml.getBytes(Charset.forName("EUC-KR"));

        // when
        List<ShRssItem> items = parser.parse(rssBytes);

        // then
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getSeq()).isEqualTo("298120");
        assertThat(items.get(0).getTitle()).isEqualTo("정상 공고");
    }

    @Test
    @DisplayName("link에 seq 파라미터가 없으면 스킵한다")
    void parse_shouldSkip_whenSeqMissingInLink() {
        // given
        String xml = rssXml(
                "<item><title>공지</title><link>http://www.i-sh.co.kr/view.do</link></item>",
                itemXml("298200", "정상", "http://www.i-sh.co.kr/view.do?seq=298200",
                        "Mon, 06 Jan 2026 12:00:00 +0900")
        );
        byte[] rssBytes = xml.getBytes(Charset.forName("EUC-KR"));

        // when
        List<ShRssItem> items = parser.parse(rssBytes);

        // then
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getSeq()).isEqualTo("298200");
    }

    @Test
    @DisplayName("pubDate가 없거나 파싱 실패면 publishedAt=null로 둔다(파싱은 계속 진행)")
    void parse_pubDateMissing_orInvalid_setsNull() {
        // given: pubDate 없음 + invalid
        String xml = rssXml(
                "<item><title>공고1</title><link>http://x/view.do?seq=1</link></item>",
                "<item><title>공고2</title><link>http://x/view.do?seq=2</link><pubDate>INVALID</pubDate></item>"
        );
        byte[] rssBytes = xml.getBytes(Charset.forName("EUC-KR"));

        // when
        List<ShRssItem> items = parser.parse(rssBytes);

        // then
        assertThat(items).hasSize(2);
        assertThat(items.get(0).getPublishedAt()).isNull();
        assertThat(items.get(1).getPublishedAt()).isNull();
    }

    @Test
    @DisplayName("RSS bytes가 null/empty면 IllegalArgumentException")
    void parse_shouldThrow_whenBytesNullOrEmpty() {
        assertThatThrownBy(() -> parser.parse(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> parser.parse(new byte[0]))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("깨진 XML이면 IllegalStateException")
    void parse_shouldThrow_whenXmlBroken() {
        // given
        byte[] broken = "<rss><channel><item>".getBytes(Charset.forName("EUC-KR"));

        // when & then
        assertThatThrownBy(() -> parser.parse(broken))
                .isInstanceOf(IllegalStateException.class);
    }

    // ===== 테스트용 XML 빌더 =====

    private static String rssXml(String... items) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"EUC-KR\"?>");
        sb.append("<rss version=\"2.0\">");
        sb.append("<channel>");
        sb.append("<title>공고 및 공지</title>");
        for (String item : items) sb.append(item);
        sb.append("</channel>");
        sb.append("</rss>");
        return sb.toString();
    }

    private static String itemXml(String seq, String title, String link, String pubDate) {
        return "<item>"
                + "<title>" + escapeXml(title) + "</title>"
                + "<link>" + escapeXml(link) + "</link>"
                + "<pubDate>" + escapeXml(pubDate) + "</pubDate>"
                + "</item>";
    }

    private static String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
