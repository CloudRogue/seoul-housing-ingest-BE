package com.seoulhousing.ingest_core.external.sh.service;

import com.seoulhousing.ingest_core.external.sh.dto.ShRssItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ShRssXmlParser 단위 테스트
 *
 * 목표:
 * - RSS 원문(byte[]) -> ShRssItem 리스트 변환이 정상 동작하는지 확인
 * - title/link 누락, seq 누락 등 "비교 불가능"한 항목은 스킵하는 정책 고정
 * - EUC-KR 인코딩/한글 포함 상황에서도 파싱이 깨지지 않는지 확인
 */
class ShRssXmlParserTest {

    private final ShRssXmlParser parser = new ShRssXmlParser();

    @Test
    @DisplayName("정상 RSS: item 2개를 파싱하고 seq/title/link를 채운다")
    void parse_shouldReturnItems_whenValidRss() {
        // given
        byte[] rssBytes = rssXml(
                itemXml("298537", "행복주택 예비자 계약결과 알림", "http://www.i-sh.co.kr/view.do?seq=298537"),
                itemXml("298522", "[임대] 청년안심주택 예비2차 입주안내", "http://www.i-sh.co.kr/view.do?seq=298522")
        ).getBytes(Charset.forName("EUC-KR"));

        // when
        List<ShRssItem> items = parser.parse(rssBytes);

        // then
        assertEquals(2, items.size());

        assertEquals("298537", items.get(0).getSeq());
        assertEquals("행복주택 예비자 계약결과 알림", items.get(0).getTitle());
        assertEquals("http://www.i-sh.co.kr/view.do?seq=298537", items.get(0).getLink());

        assertEquals("298522", items.get(1).getSeq());
        assertEquals("[임대] 청년안심주택 예비2차 입주안내", items.get(1).getTitle());
        assertEquals("http://www.i-sh.co.kr/view.do?seq=298522", items.get(1).getLink());
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
                itemXml("298120", "정상 공고", "http://www.i-sh.co.kr/view.do?seq=298120")
        );
        byte[] rssBytes = xml.getBytes(Charset.forName("EUC-KR"));

        // when
        List<ShRssItem> items = parser.parse(rssBytes);

        // then
        assertEquals(1, items.size());
        assertEquals("298120", items.get(0).getSeq());
        assertEquals("정상 공고", items.get(0).getTitle());
    }

    @Test
    @DisplayName("link에 seq 파라미터가 없으면 스킵한다")
    void parse_shouldSkip_whenSeqMissingInLink() {
        // given
        String xml = rssXml(
                "<item><title>공지</title><link>http://www.i-sh.co.kr/view.do</link></item>",
                itemXml("298200", "정상", "http://www.i-sh.co.kr/view.do?seq=298200")
        );
        byte[] rssBytes = xml.getBytes(Charset.forName("EUC-KR"));

        // when
        List<ShRssItem> items = parser.parse(rssBytes);

        // then
        assertEquals(1, items.size());
        assertEquals("298200", items.get(0).getSeq());
    }

    @Test
    @DisplayName("RSS bytes가 null/empty면 IllegalArgumentException")
    void parse_shouldThrow_whenBytesNullOrEmpty() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(null));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(new byte[0]));
    }

    @Test
    @DisplayName("깨진 XML이면 IllegalStateException")
    void parse_shouldThrow_whenXmlBroken() {
        // given
        byte[] broken = "<rss><channel><item>".getBytes(Charset.forName("EUC-KR"));

        // when & then
        assertThrows(IllegalStateException.class, () -> parser.parse(broken));
    }

    /**
     * 테스트용 RSS 루트 생성
     * - 실제 RSS 스펙 전부 필요 없음(파서는 item/title/link만 본다)
     */
    private static String rssXml(String... items) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"EUC-KR\"?>");
        sb.append("<rss version=\"2.0\">");
        sb.append("<channel>");
        sb.append("<title>공고 및 공지</title>");
        for (String item : items) {
            sb.append(item);
        }
        sb.append("</channel>");
        sb.append("</rss>");
        return sb.toString();
    }

    /**
     * 테스트용 item 생성
     */
    private static String itemXml(String seq, String title, String link) {
        return "<item>"
                + "<title>" + escapeXml(title) + "</title>"
                + "<link>" + escapeXml(link) + "</link>"
                + "</item>";
    }

    /**
     * 아주 최소한의 XML escape
     * (테스트 문자열에 &, < 등이 들어가면 파싱이 깨질 수 있어 방어)
     */
    private static String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}