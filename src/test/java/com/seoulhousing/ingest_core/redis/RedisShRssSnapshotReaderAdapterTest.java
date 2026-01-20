package com.seoulhousing.ingest_core.redis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisShRssSnapshotReaderAdapterTest {

    private static final String SOURCE = "sh";

    @Test
    void getMeta_returns_meta_map() {
        RedisTemplate<String, byte[]> redisBytesTemplate = mock(RedisTemplate.class);
        RedisTemplate<String, String> redisStringTemplate = mock(RedisTemplate.class);
        RedisKeyFactory keyFactory = mock(RedisKeyFactory.class);

        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(redisStringTemplate.opsForHash()).thenReturn(hashOps);

        when(keyFactory.metaKey(SOURCE, "notice", "all")).thenReturn("meta-key");

        Map<Object, Object> redisMeta = new LinkedHashMap<>();
        redisMeta.put("compressed", "false");
        redisMeta.put("encoding", "euc-kr");
        when(hashOps.entries("meta-key")).thenReturn(redisMeta);

        RedisShRssSnapshotReaderAdapter adapter =
                new RedisShRssSnapshotReaderAdapter(redisBytesTemplate, redisStringTemplate, keyFactory);

        Map<String, String> result = adapter.getMeta("notice", "all");

        assertThat(result).containsEntry("compressed", "false");
        assertThat(result).containsEntry("encoding", "euc-kr");
    }

    @Test
    void getSnapshotBytes_returns_payload_when_not_compressed() {
        RedisTemplate<String, byte[]> redisBytesTemplate = mock(RedisTemplate.class);
        RedisTemplate<String, String> redisStringTemplate = mock(RedisTemplate.class);
        RedisKeyFactory keyFactory = mock(RedisKeyFactory.class);

        ValueOperations<String, byte[]> valueOps = mock(ValueOperations.class);
        when(redisBytesTemplate.opsForValue()).thenReturn(valueOps);

        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(redisStringTemplate.opsForHash()).thenReturn(hashOps);

        when(keyFactory.snapshotKey(SOURCE, "notice", "all")).thenReturn("snapshot-key");
        when(keyFactory.metaKey(SOURCE, "notice", "all")).thenReturn("meta-key");

        byte[] payload = "raw rss bytes".getBytes();
        when(valueOps.get("snapshot-key")).thenReturn(payload);

        when(hashOps.entries("meta-key")).thenReturn(Map.of("compressed", "false"));

        RedisShRssSnapshotReaderAdapter adapter =
                new RedisShRssSnapshotReaderAdapter(redisBytesTemplate, redisStringTemplate, keyFactory);

        byte[] result = adapter.getSnapshotBytes("notice", "all");

        assertThat(result).isEqualTo(payload);
    }

    @Test
    void getSnapshotBytes_returns_unzipped_when_compressed_true() throws Exception {
        RedisTemplate<String, byte[]> redisBytesTemplate = mock(RedisTemplate.class);
        RedisTemplate<String, String> redisStringTemplate = mock(RedisTemplate.class);
        RedisKeyFactory keyFactory = mock(RedisKeyFactory.class);

        ValueOperations<String, byte[]> valueOps = mock(ValueOperations.class);
        when(redisBytesTemplate.opsForValue()).thenReturn(valueOps);

        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(redisStringTemplate.opsForHash()).thenReturn(hashOps);

        when(keyFactory.snapshotKey(SOURCE, "notice", "all")).thenReturn("snapshot-key");
        when(keyFactory.metaKey(SOURCE, "notice", "all")).thenReturn("meta-key");

        byte[] original = "<rss>hello</rss>".getBytes();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gos = new GZIPOutputStream(baos)) {
            gos.write(original);
        }
        byte[] gzipped = baos.toByteArray();

        when(valueOps.get("snapshot-key")).thenReturn(gzipped);
        when(hashOps.entries("meta-key")).thenReturn(Map.of("compressed", "true"));

        RedisShRssSnapshotReaderAdapter adapter =
                new RedisShRssSnapshotReaderAdapter(redisBytesTemplate, redisStringTemplate, keyFactory);

        byte[] result = adapter.getSnapshotBytes("notice", "all");

        assertThat(result).isEqualTo(original);
    }

    @Test
    void getSnapshotBytes_returns_null_when_absent() {
        RedisTemplate<String, byte[]> redisBytesTemplate = mock(RedisTemplate.class);
        RedisTemplate<String, String> redisStringTemplate = mock(RedisTemplate.class);
        RedisKeyFactory keyFactory = mock(RedisKeyFactory.class);

        ValueOperations<String, byte[]> valueOps = mock(ValueOperations.class);
        when(redisBytesTemplate.opsForValue()).thenReturn(valueOps);

        when(keyFactory.snapshotKey(SOURCE, "notice", "all")).thenReturn("snapshot-key");
        when(valueOps.get("snapshot-key")).thenReturn(null);

        RedisShRssSnapshotReaderAdapter adapter =
                new RedisShRssSnapshotReaderAdapter(redisBytesTemplate, redisStringTemplate, keyFactory);

        byte[] result = adapter.getSnapshotBytes("notice", "all");

        assertThat(result).isNull();
    }

    @Test
    void getSnapshotBytes_throws_when_meta_says_compressed_but_payload_not_gzip() {
        RedisTemplate<String, byte[]> redisBytesTemplate = mock(RedisTemplate.class);
        RedisTemplate<String, String> redisStringTemplate = mock(RedisTemplate.class);
        RedisKeyFactory keyFactory = mock(RedisKeyFactory.class);

        ValueOperations<String, byte[]> valueOps = mock(ValueOperations.class);
        when(redisBytesTemplate.opsForValue()).thenReturn(valueOps);

        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(redisStringTemplate.opsForHash()).thenReturn(hashOps);

        when(keyFactory.snapshotKey(SOURCE, "notice", "all")).thenReturn("snapshot-key");
        when(keyFactory.metaKey(SOURCE, "notice", "all")).thenReturn("meta-key");

        byte[] notGzip = "not gzip".getBytes();
        when(valueOps.get("snapshot-key")).thenReturn(notGzip);
        when(hashOps.entries("meta-key")).thenReturn(Map.of("compressed", "true"));

        RedisShRssSnapshotReaderAdapter adapter =
                new RedisShRssSnapshotReaderAdapter(redisBytesTemplate, redisStringTemplate, keyFactory);

        assertThatThrownBy(() -> adapter.getSnapshotBytes("notice", "all"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("gunzip failed");
    }
}
