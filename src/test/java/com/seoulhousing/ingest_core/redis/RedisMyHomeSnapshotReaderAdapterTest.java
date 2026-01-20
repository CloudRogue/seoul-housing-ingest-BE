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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisMyHomeSnapshotReaderAdapterTest {

    private static final String SOURCE = "myhome"; // ✅ adapter 내부 SOURCE와 동일해야 함

    @Test
    void getMeta_returns_meta_map() {
        RedisTemplate<String, byte[]> redisBytesTemplate = mock(RedisTemplate.class);
        RedisTemplate<String, String> stringRedisTemplate = mock(RedisTemplate.class);
        RedisKeyFactory keyFactory = mock(RedisKeyFactory.class);

        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOps);

        // ✅ source 포함
        when(keyFactory.metaKey(SOURCE, "rsdt", "seoul")).thenReturn("meta-key");

        Map<Object, Object> redisMeta = new LinkedHashMap<>();
        redisMeta.put("compressed", "false");
        redisMeta.put("count", "10");
        when(hashOps.entries("meta-key")).thenReturn(redisMeta);

        RedisMyHomeSnapshotReaderAdapter adapter =
                new RedisMyHomeSnapshotReaderAdapter(redisBytesTemplate, stringRedisTemplate, keyFactory);

        Map<String, String> result = adapter.getMeta("rsdt", "seoul");

        assertThat(result).containsEntry("compressed", "false");
        assertThat(result).containsEntry("count", "10");
    }

    @Test
    void getChecksum_returns_value_when_exists() {
        RedisTemplate<String, byte[]> redisBytesTemplate = mock(RedisTemplate.class);
        RedisTemplate<String, String> stringRedisTemplate = mock(RedisTemplate.class);
        RedisKeyFactory keyFactory = mock(RedisKeyFactory.class);

        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOps);

        // ✅ source 포함
        when(keyFactory.checksumKey(SOURCE, "rsdt", "seoul")).thenReturn("checksum-key");
        when(hashOps.get("checksum-key", "std-1")).thenReturn("abc123");

        RedisMyHomeSnapshotReaderAdapter adapter =
                new RedisMyHomeSnapshotReaderAdapter(redisBytesTemplate, stringRedisTemplate, keyFactory);

        String checksum = adapter.getChecksum("rsdt", "seoul", "std-1");

        assertThat(checksum).isEqualTo("abc123");
    }

    @Test
    void getChecksum_returns_null_when_absent() {
        RedisTemplate<String, byte[]> redisBytesTemplate = mock(RedisTemplate.class);
        RedisTemplate<String, String> stringRedisTemplate = mock(RedisTemplate.class);
        RedisKeyFactory keyFactory = mock(RedisKeyFactory.class);

        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOps);

        // ✅ source 포함
        when(keyFactory.checksumKey(SOURCE, "rsdt", "seoul")).thenReturn("checksum-key");
        when(hashOps.get("checksum-key", "std-1")).thenReturn(null);

        RedisMyHomeSnapshotReaderAdapter adapter =
                new RedisMyHomeSnapshotReaderAdapter(redisBytesTemplate, stringRedisTemplate, keyFactory);

        String checksum = adapter.getChecksum("rsdt", "seoul", "std-1");

        assertThat(checksum).isNull();
    }

    @Test
    void getAllChecksums_returns_map() {
        RedisTemplate<String, byte[]> redisBytesTemplate = mock(RedisTemplate.class);
        RedisTemplate<String, String> stringRedisTemplate = mock(RedisTemplate.class);
        RedisKeyFactory keyFactory = mock(RedisKeyFactory.class);

        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOps);

        // ✅ source 포함
        when(keyFactory.checksumKey(SOURCE, "rsdt", "seoul")).thenReturn("checksum-key");

        Map<Object, Object> redisData = new LinkedHashMap<>();
        redisData.put("id1", "hash1");
        redisData.put("id2", "hash2");
        when(hashOps.entries("checksum-key")).thenReturn(redisData);

        RedisMyHomeSnapshotReaderAdapter adapter =
                new RedisMyHomeSnapshotReaderAdapter(redisBytesTemplate, stringRedisTemplate, keyFactory);

        Map<String, String> result = adapter.getAllChecksums("rsdt", "seoul");

        assertThat(result).containsEntry("id1", "hash1");
        assertThat(result).containsEntry("id2", "hash2");
    }

    @Test
    void getSnapshotJsonBytes_not_compressed() {
        RedisTemplate<String, byte[]> redisBytesTemplate = mock(RedisTemplate.class);
        RedisTemplate<String, String> stringRedisTemplate = mock(RedisTemplate.class);
        RedisKeyFactory keyFactory = mock(RedisKeyFactory.class);

        ValueOperations<String, byte[]> valueOps = mock(ValueOperations.class);
        when(redisBytesTemplate.opsForValue()).thenReturn(valueOps);

        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOps);

        // ✅ source 포함
        when(keyFactory.snapshotKey(SOURCE, "rsdt", "seoul")).thenReturn("snapshot-key");
        when(keyFactory.metaKey(SOURCE, "rsdt", "seoul")).thenReturn("meta-key");

        byte[] payload = "hello".getBytes();
        when(valueOps.get("snapshot-key")).thenReturn(payload);

        when(hashOps.entries("meta-key")).thenReturn(Map.of("compressed", "false"));

        RedisMyHomeSnapshotReaderAdapter adapter =
                new RedisMyHomeSnapshotReaderAdapter(redisBytesTemplate, stringRedisTemplate, keyFactory);

        byte[] result = adapter.getSnapshotJsonBytes("rsdt", "seoul");

        assertThat(result).isEqualTo(payload);
    }

    @Test
    void getSnapshotJsonBytes_compressed() throws Exception {
        RedisTemplate<String, byte[]> redisBytesTemplate = mock(RedisTemplate.class);
        RedisTemplate<String, String> stringRedisTemplate = mock(RedisTemplate.class);
        RedisKeyFactory keyFactory = mock(RedisKeyFactory.class);

        ValueOperations<String, byte[]> valueOps = mock(ValueOperations.class);
        when(redisBytesTemplate.opsForValue()).thenReturn(valueOps);

        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOps);

        // ✅ source 포함
        when(keyFactory.snapshotKey(SOURCE, "rsdt", "seoul")).thenReturn("snapshot-key");
        when(keyFactory.metaKey(SOURCE, "rsdt", "seoul")).thenReturn("meta-key");

        byte[] original = "hello gzip".getBytes();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gos = new GZIPOutputStream(baos)) {
            gos.write(original);
        }
        byte[] gzipped = baos.toByteArray();

        when(valueOps.get("snapshot-key")).thenReturn(gzipped);
        when(hashOps.entries("meta-key")).thenReturn(Map.of("compressed", "true"));

        RedisMyHomeSnapshotReaderAdapter adapter =
                new RedisMyHomeSnapshotReaderAdapter(redisBytesTemplate, stringRedisTemplate, keyFactory);

        byte[] result = adapter.getSnapshotJsonBytes("rsdt", "seoul");

        assertThat(result).isEqualTo(original);
    }

    @Test
    void getSnapshotJsonBytes_returns_null_when_absent() {
        RedisTemplate<String, byte[]> redisBytesTemplate = mock(RedisTemplate.class);
        RedisTemplate<String, String> stringRedisTemplate = mock(RedisTemplate.class);
        RedisKeyFactory keyFactory = mock(RedisKeyFactory.class);

        ValueOperations<String, byte[]> valueOps = mock(ValueOperations.class);
        when(redisBytesTemplate.opsForValue()).thenReturn(valueOps);

        // ✅ source 포함
        when(keyFactory.snapshotKey(SOURCE, "rsdt", "seoul")).thenReturn("snapshot-key");
        when(valueOps.get("snapshot-key")).thenReturn(null);

        RedisMyHomeSnapshotReaderAdapter adapter =
                new RedisMyHomeSnapshotReaderAdapter(redisBytesTemplate, stringRedisTemplate, keyFactory);

        byte[] result = adapter.getSnapshotJsonBytes("rsdt", "seoul");

        assertThat(result).isNull();
    }
}
