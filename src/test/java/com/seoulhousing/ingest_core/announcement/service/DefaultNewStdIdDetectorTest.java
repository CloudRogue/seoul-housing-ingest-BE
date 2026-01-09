package com.seoulhousing.ingest_core.announcement.service;

import com.seoulhousing.ingest_core.announcement.port.SeenStdIdReaderPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultNewStdIdDetectorTest {

    @Mock
    SeenStdIdReaderPort seenStdIdReaderPort;

    @InjectMocks
    DefaultNewStdIdDetector detector;

    @Test
    void detectNewStdIds_seen에_없고_blank가_아닌것만_신규로_골라낸다() {
        // given
        String source = "myhome";
        String category = "rsdt";
        String scope = "seoul";

        when(seenStdIdReaderPort.getSeenStdIds(source, category, scope))
                .thenReturn(Set.of("A", "B"));

        // List.of는 null 금지라서 Arrays.asList 사용
        List<String> currentStdIds = Arrays.asList(
                "A",        // seen -> 제외
                " C ",      // 신규 -> 포함(trim)
                "",         // 제외
                "   ",      // 제외
                null,       // 제외
                "B",        // seen -> 제외
                "D"         // 신규 -> 포함
        );

        // when
        List<String> result = detector.detectNewStdIds(source, category, scope, currentStdIds);

        // then
        assertThat(result).containsExactly("C", "D");

        verify(seenStdIdReaderPort, times(1)).getSeenStdIds(source, category, scope);
        verifyNoMoreInteractions(seenStdIdReaderPort);
    }
}
