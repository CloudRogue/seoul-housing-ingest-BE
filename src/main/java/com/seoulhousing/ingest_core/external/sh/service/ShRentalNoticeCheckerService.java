package com.seoulhousing.ingest_core.external.sh.service;

import com.seoulhousing.ingest_core.external.sh.client.ShRssApiClient;
import com.seoulhousing.ingest_core.external.sh.dto.ShRssDiffResult;
import com.seoulhousing.ingest_core.external.sh.dto.ShRssItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShRentalNoticeCheckerService implements ShRentalNoticeChecker{

    private static final Logger log = LoggerFactory.getLogger(ShRentalNoticeCheckerService.class);

    private final ShRssApiClient client;
    private final ShRssXmlParser parser;
    private final ShRssRentalNoticeDiff diff;

    public ShRentalNoticeCheckerService(ShRssApiClient client, ShRssXmlParser parser, ShRssRentalNoticeDiff diff) {
        this.client = client;
        this.parser = parser;
        this.diff = diff;
    }

    @Override
    public ShRssDiffResult checkNewRentalNotice(String lastSeenSeq) {
        if (lastSeenSeq == null || lastSeenSeq.isBlank()) {
            throw new IllegalArgumentException("lastSeenSeq must not be null/blank");
        }

        // 원문을 바이트로가져오기
        byte[] rssBytes = client.fetchNoticeRssBytes();

        // 아이템리스트로 파싱
        List<ShRssItem> items = parser.parse(rssBytes);

        // 신규임대공고가 있나 확인
        ShRssDiffResult result = diff.diff(items, lastSeenSeq);

        // 운영 로그 확인
        log.info(
                "[SH][RSS] diff result. hasNewRental={}, lastSeenFound={}, lastSeenSeq={}, latestSeq={}, items={}",
                result.isHasNewRental(),          // 신규 임대 공고 있냐
                result.isLastSeenFound(),         // lastSeenSeq를 RSS에서 찾았냐(동기화 판단)
                lastSeenSeq,                      // 비교 기준값(입력)
                result.getLatestSeq(),            // RSS 상 최신 seq(다음 저장용)
                items.size()                      // 파싱된 아이템 수
        );

        return result;
    }
}
