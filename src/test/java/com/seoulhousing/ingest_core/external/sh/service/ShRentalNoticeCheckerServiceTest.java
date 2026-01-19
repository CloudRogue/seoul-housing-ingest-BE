package com.seoulhousing.ingest_core.external.sh.service;

import com.seoulhousing.ingest_core.external.sh.client.ShRssApiClient;
import com.seoulhousing.ingest_core.external.sh.dto.ShRssItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
 * - 외부 의존성(HTTP 호출, XML 파싱)은 mock 처리
 * - service가 "흐름"을 올바르게 수행하는지만 검증
 */
@ExtendWith(MockitoExtension.class)
class ShRentalNoticeCheckerServiceTest {

    @Mock
    private ShRssApiClient client;

    @Mock
    private ShRssXmlParser parser;

    @InjectMocks
    private ShRentalNoticeCheckerService service;

    @Test
    @DisplayName("정상 흐름: client->parser 호출 후 parser 결과를 그대로 반환")
    void fetchAllItems_happyPath_returnsParsedItems() {
        // given
        byte[] rssBytes = "<rss/>".getBytes();

        List<ShRssItem> items = List.of(
                new ShRssItem("298100", "임대 공고", "http://x/view.do?seq=298100", Instant.parse("2026-01-06T03:00:00Z"))
        );

        when(client.fetchNoticeRssBytes()).thenReturn(rssBytes);
        when(parser.parse(rssBytes)).thenReturn(items);

        // when
        List<ShRssItem> actual = service.fetchAllItems();

        // then
        assertThat(actual).isEqualTo(items);

        verify(client, times(1)).fetchNoticeRssBytes();
        verify(parser, times(1)).parse(rssBytes);
        verifyNoMoreInteractions(client, parser);
    }

    @Test
    @DisplayName("parser가 null을 반환하면 빈 리스트를 반환")
    void fetchAllItems_parserReturnsNull_returnsEmptyList() {
        // given
        byte[] rssBytes = "<rss/>".getBytes();

        when(client.fetchNoticeRssBytes()).thenReturn(rssBytes);
        when(parser.parse(rssBytes)).thenReturn(null);

        // when
        List<ShRssItem> actual = service.fetchAllItems();

        // then
        assertThat(actual).isNotNull();
        assertThat(actual).isEmpty();

        verify(client, times(1)).fetchNoticeRssBytes();
        verify(parser, times(1)).parse(rssBytes);
        verifyNoMoreInteractions(client, parser);
    }
}
