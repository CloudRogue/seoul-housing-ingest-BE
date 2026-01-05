package com.seoulhousing.ingest_core.external.myhome.service;

import com.seoulhousing.ingest_core.external.myhome.client.MyHomeApiClient;
import com.seoulhousing.ingest_core.external.myhome.dto.MyHomeItemDto;
import com.seoulhousing.ingest_core.external.myhome.dto.RsdtListRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MyHomeAnnouncementCollectService {

    private static final Logger log = LoggerFactory.getLogger(MyHomeAnnouncementCollectService.class);

    private final MyHomeApiClient myHomeApiClient;

    public MyHomeAnnouncementCollectService(MyHomeApiClient myHomeApiClient) {
        this.myHomeApiClient = myHomeApiClient;
    }

    //공공임대 공고를 전부 가져온뒤 , 원하는 지역만 필터링해서 반환하기
    public List<MyHomeItemDto> collectRsdt(RsdtListRequest baseRequest RegionFilter filter) {}


    public static class RegionFilter{

        private final Set<String> brtcNormalized;

        public RegionFilter(Set<String> brtcNormalized) {
            this.brtcNormalized = brtcNormalized;
        }

        // 전국(혹시 확장성이 있을수도 있으니 설정)
        public static RegionFilter all() {
            return new RegionFilter(Collections.emptySet());
        }

        // 시도명으로 필터 생성
        public static RegionFilter ofBrtcPrefixes(Collection<String> prefixes) {
            if(prefixes == null || prefixes.isEmpty()) return all();

            Set<String> normalized = prefixes.stream()
                    .filter(Objects::nonNull)
                    .map(RegionFilter::normalize)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            return new RegionFilter(normalized);
        }

        // 실제로 아이템이 필터에 매칭이 되었는지 판정하는 함수
        public boolean matches(MyHomeItemDto item) {
            if(brtcNormalized)
        }
    }
}
