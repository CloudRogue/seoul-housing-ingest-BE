package com.seoulhousing.ingest_core.announcement.service;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultNewStdIdDetectorTest {

    private final DefaultNewStdIdDetector detector = new DefaultNewStdIdDetector();

    @Test
    void detect_seen에_없고_blank가_아닌것만_신규로_골라낸다() {
        // given
        Set<String> seen = Set.of("A", "B");

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
        List<String> result = detector.detect(seen, currentStdIds);

        // then
        assertThat(result).containsExactly("C", "D");
    }

    @Test
    void detect_current가_null이면_빈리스트() {
        // when
        List<String> result = detector.detect(Set.of("A"), null);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void detect_current가_비어있으면_빈리스트() {
        // when
        List<String> result = detector.detect(Set.of("A"), List.of());

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void detect_seen이_null이면_전부신규로간주() {
        // given
        List<String> currentStdIds = Arrays.asList("A", " B ", null, "  ");

        // when
        List<String> result = detector.detect(null, currentStdIds);

        // then
        assertThat(result).containsExactly("A", "B");
    }
}
