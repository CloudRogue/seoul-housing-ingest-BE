package com.seoulhousing.ingest_core.external.sh.service;

import com.seoulhousing.ingest_core.external.sh.client.ShRssApiClient;
import com.seoulhousing.ingest_core.external.sh.dto.ShRssDiffResult;
import com.seoulhousing.ingest_core.external.sh.dto.ShRssItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ShRentalNoticeCheckerService 단위테스트
 *
 * 목표:
 * - 외부 의존성(HTTP 호출, XML 파싱, 비교 로직)은 mock 처리
 * - service가 "흐름"을 올바르게 수행하는지만 검증
 */
@ExtendWith(MockitoExtension.class)
class ShRentalNoticeCheckerServiceTest {

    @Mock
    private ShRssApiClient client;

    @Mock
    private ShRssXmlParser parser;

    @Mock
    private ShRssRentalNoticeDiff diff;

    @InjectMocks
    private ShRentalNoticeCheckerService service;

    @Test
    @DisplayName("lastSeenSeq가 null이면 예외 + 의존성 호출 없음")
    void checkNewRentalNotice_nullLastSeen_throws() {
        assertThatThrownBy(() -> service.checkNewRentalNotice(null))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(client, parser, diff);
    }

    @Test
    @DisplayName("lastSeenSeq가 blank면 예외 + 의존성 호출 없음")
    void checkNewRentalNotice_blankLastSeen_throws() {
        assertThatThrownBy(() -> service.checkNewRentalNotice("   "))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(client, parser, diff);
    }

    @Test
    @DisplayName("정상 흐름: client->parser->diff 호출 후 diff 결과를 그대로 반환")
    void checkNewRentalNotice_happyPath_returnsDiffResult() {
        // given
        String lastSeenSeq = "298000";

        byte[] rssBytes = "<rss/>".getBytes();
        List<ShRssItem> items = List.of(
                new ShRssItem("298100", "임대 공고", "http://x/view.do?seq=298100", Instant.parse("2026-01-06T03:00:00Z"))
        );

        ShRssDiffResult expected = new ShRssDiffResult(
                true,
                true,
                "298100",
                List.of(items.get(0))
        );

        when(client.fetchNoticeRssBytes()).thenReturn(rssBytes);
        when(parser.parse(rssBytes)).thenReturn(items);
        when(diff.diff(items, lastSeenSeq)).thenReturn(expected);

        // when
        ShRssDiffResult actual = service.checkNewRentalNotice(lastSeenSeq);

        // then
        assertThat(actual).isSameAs(expected);

        verify(client, times(1)).fetchNoticeRssBytes();
        verify(parser, times(1)).parse(rssBytes);
        verify(diff, times(1)).diff(items, lastSeenSeq);
        verifyNoMoreInteractions(client, parser, diff);
    }

    @Test
    @DisplayName("parser가 만든 items가 diff로 그대로 전달되는지 캡쳐로 검증")
    void checkNewRentalNotice_passesItemsToDiff() {
        // given
        String lastSeenSeq = "298000";

        byte[] rssBytes = "<rss/>".getBytes();
        List<ShRssItem> items = List.of(
                new ShRssItem("298101", "임대", "http://x/view.do?seq=298101", Instant.parse("2026-01-06T04:00:00Z")),
                new ShRssItem("298100", "공지", "http://x/view.do?seq=298100", Instant.parse("2026-01-06T03:00:00Z"))
        );

        ShRssDiffResult expected = new ShRssDiffResult(
                false,
                true,
                "298101",
                List.of()
        );

        when(client.fetchNoticeRssBytes()).thenReturn(rssBytes);
        when(parser.parse(rssBytes)).thenReturn(items);
        when(diff.diff(anyList(), eq(lastSeenSeq))).thenReturn(expected);

        // when
        ShRssDiffResult actual = service.checkNewRentalNotice(lastSeenSeq);

        // then
        assertThat(actual).isSameAs(expected);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ShRssItem>> captor = ArgumentCaptor.forClass(List.class);

        verify(diff, times(1)).diff(captor.capture(), eq(lastSeenSeq));
        assertThat(captor.getValue()).isEqualTo(items);
    }
}
