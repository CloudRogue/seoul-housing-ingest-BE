package com.seoulhousing.ingest_core.external.myhome.redis;

import com.seoulhousing.ingest_core.external.myhome.dto.MyHomeItemDto;
import com.seoulhousing.ingest_core.redis.MyHomeSnapShotStore;
import com.seoulhousing.ingest_core.redis.RedisKeyFactory;
import com.seoulhousing.ingest_core.redis.config.IngestSnapShotProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MyHomeSnapShotStoreRedisTest {

    @Autowired
    MyHomeSnapShotStore store;

    @Autowired
    RedisKeyFactory keyFactory;

    // 내가 만든 템플릿들 실제 주입
    @Autowired RedisTemplate<String, byte[]> redisBytesTemplate;
    @Autowired RedisTemplate<String, String> redisTemplate;

    @Autowired IngestSnapShotProperties props;

    private String category = "rsdt";
    private String scope = "seoul";

    private String snapshotKey;
    private String checksumKey;
    private String metaKey;

    @BeforeEach
    void setUp() {
        Assumptions.assumeTrue(Boolean.TRUE.equals(redisTemplate.getConnectionFactory().getConnection().ping() != null));

        snapshotKey = keyFactory.snapshotKey(category, scope);
        checksumKey = keyFactory.checksumKey(category, scope);
        metaKey = keyFactory.metaKey(category, scope);

        redisTemplate.delete(Set.of(checksumKey, metaKey));
        redisBytesTemplate.delete(snapshotKey);
    }

    @AfterEach
    void tearDown() {
        // 테스트 끝나면 정리
        redisTemplate.delete(Set.of(checksumKey, metaKey));
        redisBytesTemplate.delete(snapshotKey);
    }

    @Test
    void save_should_store_snapshot_checksum_meta_in_real_redis() throws Exception {
        // given:
        MyHomeItemDto item = new MyHomeItemDto();
        setField(item, "pblancId", "99999");
        setField(item, "houseSn", "1");

        // when
        store.save(category, scope, List.of(item));

        // then 1) 키 존재 확인
        assertThat(redisBytesTemplate.hasKey(snapshotKey)).isTrue();
        assertThat(redisTemplate.hasKey(checksumKey)).isTrue();
        assertThat(redisTemplate.hasKey(metaKey)).isTrue();

        // then 2) checksum field 개수 확인(1개여야 함)
        Long hlen = redisTemplate.opsForHash().size(checksumKey);
        assertThat(hlen).isEqualTo(1);

        // then 3) meta 필드들 존재 확인
        Object count = redisTemplate.opsForHash().get(metaKey, "count");
        Object serializer = redisTemplate.opsForHash().get(metaKey, "serializer");
        assertThat(count).isEqualTo("1");
        assertThat(serializer).isEqualTo("jackson");

        // then 4) TTL 확인 (대략 7일 근처)
        long ttlSeconds = redisTemplate.getExpire(metaKey);
        long expected = Duration.ofDays(props.getTtlDays()).getSeconds();

        // 환경/실행 시간 차 감안해서 범위로 체크(±30초)
        assertThat(ttlSeconds).isBetween(expected - 30, expected);

        // snapshot도 TTL 확인
        long ttlSnapshot = redisBytesTemplate.getExpire(snapshotKey);
        assertThat(ttlSnapshot).isBetween(expected - 30, expected);
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}