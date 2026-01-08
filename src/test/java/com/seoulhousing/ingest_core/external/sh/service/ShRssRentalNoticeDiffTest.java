package com.seoulhousing.ingest_core.external.sh.service;

import com.seoulhousing.ingest_core.external.sh.dto.ShRssDiffResult;
import com.seoulhousing.ingest_core.external.sh.dto.ShRssItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * ShRssRentalNoticeDiff 단위 테스트
 *
 * 목적:
 * - items 정렬(pubDate desc, seq desc)을 내부에서 보장한다.
 * - lastSeenSeq "이전(신규 구간)"만 보면서 임대("임대" 키워드) 신규를 판별한다.
 * - 신규 임대 아이템 목록(newRentalItems)을 반환한다.
 *
 * 핵심 규칙(현재 구현 기준):
 * 1) 정렬: publishedAt desc(null last), seq(long) desc(null last)
 * 2) lastSeenSeq를 만나면 stop
 * 3) lastSeenSeq를 못 찾으면 lastSeenFound=false, hasNewRental=false, newRentalItems=[]
 * 4) latestSeq는 정렬 후 0번 아이템의 seq (없으면 null)
 */
class ShRssRentalNoticeDiffTest {

    private final ShRssRentalNoticeDiff diff = new ShRssRentalNoticeDiff();

    @Test
    @DisplayName("정렬이 섞여 들어와도 신규 구간에 임대가 있으면 true + 신규임대 리스트 반환")
    void diff_hasNewRental_true_and_returnsNewItems() {
        // given (일부러 순서 섞음)
        ShRssItem a = item("298110", "일반 공지", "http://x/view.do?seq=298110", Instant.parse("2026-01-06T03:00:00Z"));
        ShRssItem b = item("298120", "[임대] 행복주택 모집", "http://x/view.do?seq=298120", Instant.parse("2026-01-06T04:00:00Z"));
        ShRssItem c = item("298100", "기준 글", "http://x/view.do?seq=298100", Instant.parse("2026-01-06T02:00:00Z"));
        ShRssItem past = item("298090", "[임대] 과거 글", "http://x/view.do?seq=298090", Instant.parse("2026-01-06T01:00:00Z"));

        List<ShRssItem> items = List.of(a, past, c, b);

        // when
        ShRssDiffResult result = diff.diff(items, "298100");

        // then
        assertThat(result.isLastSeenFound()).isTrue();
        assertThat(result.isHasNewRental()).isTrue();
        assertThat(result.getLatestSeq()).isEqualTo("298120");

        // 신규 구간(298120, 298110) 중 임대는 298120만
        assertThat(result.getNewRentalItems())
                .extracting(ShRssItem::getSeq)
                .containsExactly("298120");
    }

    @Test
    @DisplayName("신규 구간에 임대가 없으면 false + newRentalItems 비어있음")
    void diff_hasNewRental_false_whenNoRentalInNewSegment() {
        // given
        List<ShRssItem> items = List.of(
                item("298120", "일반 공지 1", "http://x/view.do?seq=298120", Instant.parse("2026-01-06T04:00:00Z")),
                item("298110", "일반 공지 2", "http://x/view.do?seq=298110", Instant.parse("2026-01-06T03:00:00Z")),
                item("298100", "기준 글", "http://x/view.do?seq=298100", Instant.parse("2026-01-06T02:00:00Z")),
                item("298090", "[임대] 과거 글", "http://x/view.do?seq=298090", Instant.parse("2026-01-06T01:00:00Z"))
        );

        // when
        ShRssDiffResult result = diff.diff(items, "298100");

        // then
        assertThat(result.isLastSeenFound()).isTrue();
        assertThat(result.isHasNewRental()).isFalse();
        assertThat(result.getLatestSeq()).isEqualTo("298120");
        assertThat(result.getNewRentalItems()).isEmpty();
    }

    @Test
    @DisplayName("lastSeenSeq를 못 찾으면 오탐 방지: lastSeenFound=false, hasNewRental=false, newRentalItems=[]")
    void diff_lastSeenNotFound_policy() {
        // given
        List<ShRssItem> items = List.of(
                item("298120", "[임대] 행복주택 모집", "http://x/view.do?seq=298120", Instant.parse("2026-01-06T04:00:00Z")),
                item("298110", "일반 공지", "http://x/view.do?seq=298110", Instant.parse("2026-01-06T03:00:00Z"))
        );

        // when
        ShRssDiffResult result = diff.diff(items, "298999");

        // then
        assertThat(result.isLastSeenFound()).isFalse();
        assertThat(result.isHasNewRental()).isFalse();
        assertThat(result.getLatestSeq()).isEqualTo("298120");
        assertThat(result.getNewRentalItems()).isEmpty();
    }

    @Test
    @DisplayName("items가 비어있으면 latestSeq=null, lastSeenFound=false, hasNewRental=false")
    void diff_emptyItems() {
        // when
        ShRssDiffResult result = diff.diff(List.of(), "298100");

        // then
        assertThat(result.getLatestSeq()).isNull();
        assertThat(result.isLastSeenFound()).isFalse();
        assertThat(result.isHasNewRental()).isFalse();
        assertThat(result.getNewRentalItems()).isEmpty();
    }

    private static ShRssItem item(String seq, String title, String link, Instant publishedAt) {
        return new ShRssItem(seq, title, link, publishedAt);
    }
}
