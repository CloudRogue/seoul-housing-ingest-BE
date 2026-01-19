package com.seoulhousing.ingest_core.announcement.service;

import com.seoulhousing.ingest_core.announcement.dto.ChangeDetectionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Tag("it")
@ActiveProfiles("local") // envProvider.envKey()가 local로 고정되게
class DefaultAnnouncementChangeDetectionServiceRedisIT {

    @Autowired
    DefaultAnnouncementChangeDetectionService service;

    @Autowired
    @Qualifier("redisStringTemplate")
    RedisTemplate<String, String> redisStringTemplate;

    private static final String SOURCE = "myhome";
    private static final String CATEGORY = "rsdt";
    private static final String SCOPE = "seoul";

    private String seenKey() {
        // RedisSeenStdIdReaderAdapter.seenKey 규칙 그대로
        return "seoulhousing:local:seen:" + SOURCE + ":" + CATEGORY + ":" + SCOPE;
    }

    @BeforeEach
    void cleanup() {
        redisStringTemplate.delete(seenKey());
    }

    @Test
    void detect_shouldReturnNewStdIds_whenCurrentHasUnseen() {
        // given: seen = {A, B}
        redisStringTemplate.opsForSet().add(seenKey(), "A", "B");

        // when: current = [A, B, C]
        ChangeDetectionResult res = service.detect(SOURCE, CATEGORY, SCOPE, List.of("A", "B", "C"));

        // then
        assertThat(res.getNewStdIds()).containsExactly("C");
        assertThat(res.getMissingStdIds()).isEmpty();
        assertThat(res.getCurrentCount()).isEqualTo(3);
        assertThat(res.getSeenCount()).isEqualTo(2);
    }

    @Test
    void detect_shouldReturnMissingStdIds_whenCurrentMissesSeenOnes() {
        // given: seen = {A, B, C}
        redisStringTemplate.opsForSet().add(seenKey(), "A", "B", "C");

        // when: current = [A, C]
        ChangeDetectionResult res = service.detect(SOURCE, CATEGORY, SCOPE, List.of("A", "C"));

        // then
        assertThat(res.getNewStdIds()).isEmpty();
        assertThat(res.getMissingStdIds()).containsExactly("B");
        assertThat(res.getCurrentCount()).isEqualTo(2);
        assertThat(res.getSeenCount()).isEqualTo(3);
    }

    @Test
    void detect_shouldTreatAllAsNew_whenSeenIsEmpty() {
        // given: seen empty (redis key 없음)

        // when
        ChangeDetectionResult res = service.detect(SOURCE, CATEGORY, SCOPE, List.of("A", "B"));

        // then
        assertThat(res.getNewStdIds()).containsExactly("A", "B");
        assertThat(res.getMissingStdIds()).isEmpty();
        assertThat(res.getCurrentCount()).isEqualTo(2);
        assertThat(res.getSeenCount()).isEqualTo(0);
    }

    @Test
    void detect_shouldReturnMissingAllSeen_whenCurrentIsEmpty() {
        // given
        redisStringTemplate.opsForSet().add(seenKey(), "A", "B");

        // when
        ChangeDetectionResult res = service.detect(SOURCE, CATEGORY, SCOPE, List.of());

        // then
        assertThat(res.getNewStdIds()).isEmpty();
        assertThat(res.getMissingStdIds()).containsExactlyInAnyOrder("A", "B");
        assertThat(res.getCurrentCount()).isEqualTo(0);
        assertThat(res.getSeenCount()).isEqualTo(2);
    }
}
