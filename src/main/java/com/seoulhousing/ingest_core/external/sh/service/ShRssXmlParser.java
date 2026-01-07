package com.seoulhousing.ingest_core.external.sh.service;

import com.seoulhousing.ingest_core.external.sh.dto.ShRssItem;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

// 원문을 XML로 파싱해 리스트로 변환
@Component
public class ShRssXmlParser {

    // 원문을 리스트로 변환하기
    public List<ShRssItem> parse(byte[] rssBytes)  {
        if (rssBytes == null || rssBytes.length == 0) {
            throw new IllegalArgumentException("rssBytes must not be null/empty");
        }

        //파싱결과를 담을 문서
        Document doc;
        try{
            //실제 파서 생성하고 돔트리 만들기 인풋스트림으로 감싸기
            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(rssBytes));
        }
        catch (Exception e){
            throw new IllegalStateException("SH RSS XML parse failed", e);
        }

        //공고가 아이템 단위로 반복됨 그래서 아이템 태그 통으로 가져오기
        NodeList nodes = doc.getElementsByTagName("item");

        List<ShRssItem> items = new ArrayList<>(nodes.getLength());

        for (int i = 0; i < nodes.getLength(); i++) {
            Element item = (Element) nodes.item(i);

            String title = text(item, "title");
            String link  = text(item, "link");

            //타이틀이랑 링크가 없으면 비교판별에 사용불가능하므로 그냥 건너뜀
            if (title == null || link == null) continue;

            String seq = extractSeq(link);
            if (seq == null) continue;

            items.add(new ShRssItem(seq, title, link));
        }

        return items;
    }

    private static String text(Element parent, String tag){
        //해당 tag 전부 찾고 없으면 널처리
        NodeList nl = parent.getElementsByTagName(tag);
        if (nl.getLength() == 0) return null;

        String v = nl.item(0).getTextContent();

        //정규화해주기
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    private static String extractSeq(String link) {
        try {
            URI uri = URI.create(link.trim());

            // 쿼리가 없으면 seq도 없음
            String query = uri.getRawQuery();
            if (query == null) return null;

            // & 기준으로 키밸류 조회
            for (String s : query.split("&")) {

                int index = s.indexOf("=");
                if (index < 0) continue;

                String key = s.substring(0, index);
                String value = s.substring(index + 1);

                if ("seq".equalsIgnoreCase(key)) return value;
            }
            return null;

        } catch (Exception e) {
            return null;
        }
    }
}
