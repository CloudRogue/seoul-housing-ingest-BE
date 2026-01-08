package com.seoulhousing.ingest_core.external.sh.service;

import org.springframework.stereotype.Component;

import java.net.URI;


//seq 추출하는 메서드
@Component
public class ShRssSeqExtractor {

    public String extractSeq(String link){
        if(link == null) return null;

        try{
            URI uri = URI.create(link.trim());
            String query = uri.getQuery();

            if(query == null) return null;

            for (String s : query.split("&")) {

                int index = s.indexOf("=");
                if(index < 0) continue;

                String key = s.substring(0, index);
                String value = s.substring(index + 1);

                if("seq".equalsIgnoreCase(key)) return value;
            }

            return null;
        }catch (Exception e){
            return null;
        }
    }
}
