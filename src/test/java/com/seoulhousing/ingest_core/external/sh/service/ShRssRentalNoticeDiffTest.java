package com.seoulhousing.ingest_core.external.sh.service;


import com.seoulhousing.ingest_core.external.sh.dto.ShRssDiffResult;
import com.seoulhousing.ingest_core.external.sh.dto.ShRssItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ShRssRentalNoticeDiff 단위 테스트
 *
 * 목적:
 * - "lastSeenSeq 이후 구간"만 보면서
 * - 신규 임대("임대" 키워드) 공고가 있는지 판단하는 정책을 고정한다.
 *
 * 핵심 규칙(현재 구현 기준):
 * 1) items는 최신이 앞(0번)이라고 가정한다.
 * 2) lastSeenSeq를 만나면 그 아래는 과거이므로 비교를 멈춘다.
 * 3) lastSeenSeq를 못 찾으면 (RSS 범위 밖/저장값 불일치) -> lastSeenFound=false, hasNewRental=false 반환
 * 4) latestSeq는 items[0].seq (없으면 null)
 */
class ShRssRentalNoticeDiffTest {

    private final ShRssRentalNoticeDiff diff = new ShRssRentalNoticeDiff();

    @Test
    @DisplayName("lastSeenSeq 이전(신규 구간)에 '임대'가 있으면 hasNewRental=true, lastSeenFound=true")
    void diff_hasNewRental_true_whenRentalExistsBeforeLastSeen() {
        // given: 최신 -> 과거 순서라고 가정
        List<ShRssItem> items = List.of(
                item("298120", "[임대] 행복주택 모집공고", "http://x/view.do?seq=298120"),
                item("298110", "일반 공지", "http://x/view.do?seq=298110"),
                item("298100", "기준 글(여기까지 처리)", "http://x/view.do?seq=298100"),
                item("298090", "[임대] 과거 글(보면 안 됨)", "http://x/view.do?seq=298090")
        );

        // when
        ShRssDiffResult result = diff.diff(items, "298100");

        // then
        assertTrue(result.isHasNewRental(), "lastSeenSeq 이전 신규 구간에 임대가 있으니 true여야 함");
        assertTrue(result.isLastSeenFound(), "lastSeenSeq를 리스트에서 찾았으니 true여야 함");
        assertEquals("298120", result.getLatestSeq(), "latestSeq는 items[0].seq 여야 함");
    }

    @Test
    @DisplayName("lastSeenSeq 이전(신규 구간)에 '임대'가 하나도 없으면 hasNewRental=false, lastSeenFound=true")
    void diff_hasNewRental_false_whenNoRentalExistsBeforeLastSeen() {
        // given
        List<ShRssItem> items = List.of(
                item("298120", "일반 공지 1", "http://x/view.do?seq=298120"),
                item("298110", "일반 공지 2", "http://x/view.do?seq=298110"),
                item("298100", "기준 글(여기까지 처리)", "http://x/view.do?seq=298100"),
                item("298090", "[임대] 과거 글(보면 안 됨)", "http://x/view.do?seq=298090")
        );

        // when
        ShRssDiffResult result = diff.diff(items, "298100");

        // then
        assertFalse(result.isHasNewRental(), "신규 구간에 임대가 없으니 false여야 함");
        assertTrue(result.isLastSeenFound(), "lastSeenSeq를 찾았으니 true여야 함");
        assertEquals("298120", result.getLatestSeq());
    }

    @Test
    @DisplayName("lastSeenSeq를 만나면 그 아래(과거)에서 임대가 있어도 신규로 잡히면 안 된다(Stop 규칙)")
    void diff_shouldStopWhenLastSeenFound() {
        // given: lastSeenSeq 아래에 임대가 있어도 '과거'라서 보면 안 됨
        List<ShRssItem> items = List.of(
                item("298120", "일반 공지 1", "http://x/view.do?seq=298120"),
                item("298110", "기준 글(여기까지 처리)", "http://x/view.do?seq=298110"),
                item("298100", "[임대] 과거 글(보면 안 됨)", "http://x/view.do?seq=298100")
        );

        // when
        ShRssDiffResult result = diff.diff(items, "298110");

        // then
        assertFalse(result.isHasNewRental(), "lastSeenSeq 이후(위쪽)에는 임대가 없으므로 false여야 함");
        assertTrue(result.isLastSeenFound());
        assertEquals("298120", result.getLatestSeq());
    }

    @Test
    @DisplayName("lastSeenSeq를 못 찾으면 lastSeenFound=false, hasNewRental=false, latestSeq는 최신값 유지")
    void diff_lastSeenNotFound_policy() {
        // given: lastSeenSeq=298999는 목록에 없음
        List<ShRssItem> items = List.of(
                item("298120", "[임대] 행복주택 모집공고", "http://x/view.do?seq=298120"),
                item("298110", "일반 공지", "http://x/view.do?seq=298110")
        );

        // when
        ShRssDiffResult result = diff.diff(items, "298999");

        // then
        assertFalse(result.isHasNewRental(), "lastSeenSeq를 못 찾으면 오탐 방지로 false여야 함(현 정책)");
        assertFalse(result.isLastSeenFound(), "lastSeenSeq를 못 찾았으니 false여야 함");
        assertEquals("298120", result.getLatestSeq(), "latestSeq는 items[0].seq 여야 함");
    }

    @Test
    @DisplayName("items가 비어있으면 latestSeq=null, lastSeenFound=false, hasNewRental=false")
    void diff_emptyItems() {
        // given
        List<ShRssItem> items = List.of();

        // when
        ShRssDiffResult result = diff.diff(items, "298100");

        // then
        assertFalse(result.isHasNewRental());
        assertFalse(result.isLastSeenFound());
        assertNull(result.getLatestSeq());
    }

    private static ShRssItem item(String seq, String title, String link) {
        return new ShRssItem(seq, title, link);
    }
}