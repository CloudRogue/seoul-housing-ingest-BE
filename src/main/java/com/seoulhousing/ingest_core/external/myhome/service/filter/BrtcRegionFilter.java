package com.seoulhousing.ingest_core.external.myhome.service.filter;

import com.seoulhousing.ingest_core.external.myhome.dto.MyHomeItemDto;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class BrtcRegionFilter implements RegionFilter {

    private final Set<String> prefixesNormalized;

    public BrtcRegionFilter(Collection<String> prefixes){

        // 입력을 정규화하여 안전하게 보관하기
        Set<String> set = new LinkedHashSet<>();
        if(prefixes != null){
            for (String p : prefixes) {
                if(p == null) continue;
                String n = normalize(p);
                if(!n.isBlank()) set.add(n);
            }
        }

        if (set.isEmpty()) {
            throw new IllegalArgumentException("BrtcRegionFilter prefixes must not be empty. Use AllRegionFilter for ALL scope.");
        }

        this.prefixesNormalized = Set.copyOf(set);
    }

    @Override
    public boolean matches(MyHomeItemDto item) {

        String brtc = normalize(item == null ? null : item.getBrtcNm());
        if (brtc.isBlank()) return false;

        for (String prefix : prefixesNormalized) {
            if (brtc.startsWith(prefix)) return true;
        }
        return false;
    }


    // 혹시 모르는 공백형태 제거해주는 최소 정규화 적용하기
    private static String normalize(String s) {
        if (s == null) return "";
        return s.trim().replace(" ", "");
    }
}
