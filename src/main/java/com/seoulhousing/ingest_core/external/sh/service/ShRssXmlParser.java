package com.seoulhousing.ingest_core.external.sh.service;


import com.seoulhousing.ingest_core.external.sh.dto.ShRssItem;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// 원문을 스트리밍 방식으로 파싱해서 리스트로 변환하기
@Component
public class ShRssXmlParser {


    private static final DateTimeFormatter PUBDATE_FMT =
            DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.ENGLISH);


    private final ShRssSeqExtractor seqExtractor;

    public ShRssXmlParser(ShRssSeqExtractor seqExtractor) {
        this.seqExtractor = seqExtractor;
    }

    public List<ShRssItem> parse(byte[] rssBytes){
        if(rssBytes == null || rssBytes.length == 0){
            throw new IllegalArgumentException("rssBytes is null or empty");
        }

        String encoding = detectXmlEncodingOrNull(rssBytes);

        //StAx파서를 만들기 위한 팩토리
        XMLInputFactory factory = XMLInputFactory.newInstance();

        // XXE 및 DTD 차단
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);

        List<ShRssItem> items = new ArrayList<>();
        XMLStreamReader reader = null;

        //바이트배열을 InputStram처럼 파서에 공급
        try (ByteArrayInputStream in = new ByteArrayInputStream(rssBytes)) {
            reader = (encoding == null) ? factory.createXMLStreamReader(in) : factory.createXMLStreamReader(in, encoding);

            String curTitle = null;
            String curLink = null;
            String curPubDateText = null;
            Instant curPubDate = null;
            boolean inItem = false;
            String currentTag = null;

            while (reader.hasNext()) {
                int event = reader.next();

                //태그시작
                if (event == XMLStreamConstants.START_ELEMENT) {
                    currentTag = reader.getLocalName();

                    if ("item".equals(currentTag)) {
                        inItem = true;
                        curTitle = null;
                        curLink = null;
                        curPubDateText = null;
                        curPubDate = null;
                    }
                    continue;
                }

                if ((event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA)
                        && inItem && currentTag != null) {

                    String text = reader.getText();
                    if (text != null) text = text.trim();
                    if (text == null || text.isBlank()) continue;

                    switch (currentTag) {
                        case "title" -> curTitle = append(curTitle, text);
                        case "link" -> curLink = append(curLink, text);
                        case "pubDate" -> curPubDateText = append(curPubDateText, text); // 🔥 누적
                    }
                }

                // 태그 종료
                if (event == XMLStreamConstants.END_ELEMENT) {
                    String endTag = reader.getLocalName();

                    // pubDate 닫힐 때 문자열 → Instant 변환
                    if ("pubDate".equals(endTag)) {
                        curPubDate = parsePubDateOrNull(curPubDateText);
                    }

                    // item 끝났을 때 아이템 생성
                    if ("item".equals(endTag)) {
                        inItem = false;

                        if (curPubDate == null && curPubDateText != null) {
                            curPubDate = parsePubDateOrNull(curPubDateText);
                        }

                        if (curTitle != null && curLink != null) {
                            String seq = seqExtractor.extractSeq(curLink);
                            if (seq != null) {
                                items.add(new ShRssItem(seq, curTitle, curLink, curPubDate));
                            }
                        }
                    }

                    currentTag = null;

                }
            }
            return  items;
        } catch (Exception e) {
            throw new IllegalStateException("SH RSS XML parse failed", e);
        } finally {
            if (reader != null) {
                try { reader.close(); } catch (Exception ignored) {}
            }
        }
    }

    // 문자열을 Instant로 파싱
    private static Instant parsePubDateOrNull(String raw) {
        if (raw == null) return null;
        String v = raw.trim();
        if (v.isEmpty()) return null;

        try {
            return ZonedDateTime.parse(v, PUBDATE_FMT).toInstant();
        } catch (Exception e) {
            return null;
        }
    }

    //stax에서 캐릭터이벤트가 여러번 나오는경우를 대비
    private static String append(String oldV, String add) {
        if (oldV == null) return add;
        return oldV + add;
    }

    private static String detectXmlEncodingOrNull(byte[] bytes) {
        int len = Math.min(bytes.length, 300);

        // 엔코딩 위치찾기
        String head = new String(bytes, 0, len, StandardCharsets.UTF_8);

        int idx = head.indexOf("encoding=");
        if (idx < 0) return null;

        // " 와 ' 둘 다 지원
        int q1 = head.indexOf('"', idx);
        int s1 = head.indexOf('\'', idx);

        int start = -1;
        char quote;

        // 더 먼저 등장한 따옴표를 선택
        if (q1 >= 0 && (s1 < 0 || q1 < s1)) {
            start = q1;
            quote = '"';
        } else if (s1 >= 0) {
            start = s1;
            quote = '\'';
        } else {
            return null;
        }

        // 닫는 따옴표 위치
        int end = head.indexOf(quote, start + 1);
        if (end < 0) return null;

        // 따옴표 사이의 encoding 문자열을  추출 한다
        String enc = head.substring(start + 1, end).trim();

        // 비어있으면 null 처리
        return enc.isBlank() ? null : enc;
    }
}


