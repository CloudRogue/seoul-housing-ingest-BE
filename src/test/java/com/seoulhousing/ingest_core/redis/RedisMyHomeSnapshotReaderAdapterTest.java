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

    // getMeta가 Redis HASH(meta)를 읽어서 Map<String,String> 형태로 반환하는지 테스트합니다.
    @Test
    void getMeta_returns_meta_map() {
        //  의존성 mock 생성
        RedisTemplate<String, byte[]> redisBytesTemplate = mock(RedisTemplate.class);
        RedisTemplate<String, String> stringRedisTemplate = mock(RedisTemplate.class);
        RedisKeyFactory keyFactory = mock(RedisKeyFactory.class);

        // opsForHash()가 반환할 HashOperations mock
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOps);

        //  metaKey 생성 규칙을 keyFactory가 만들도록 스텁
        when(keyFactory.metaKey("rsdt", "seoul")).thenReturn("meta-key");

        // 레디스에 있다고 가정할 원본데이터
        Map<Object, Object> redisMeta = new LinkedHashMap<>();
        redisMeta.put("compressed", "false");
        redisMeta.put("count", "10");
        when(hashOps.entries("meta-key")).thenReturn(redisMeta);

        //객체생성하고 메서드호출
        RedisMyHomeSnapshotReaderAdapter adapter =
                new RedisMyHomeSnapshotReaderAdapter(redisBytesTemplate, stringRedisTemplate, keyFactory);

        Map<String, String> result = adapter.getMeta("rsdt", "seoul");

        //  Object,Object를 String,String으로 변환해 반환하는지 확인
        assertThat(result).containsEntry("compressed", "false");
        assertThat(result).containsEntry("count", "10");
    }

    // getChecksum이 Redis HASH(checksum)에서 특정 stdId의 체크섬 값을 정상 반환하는지 테스트합니다.
    @Test
    void getChecksum_returns_value_when_exists() {
        //  mock
        RedisTemplate<String, byte[]> redisBytesTemplate = mock(RedisTemplate.class);
        RedisTemplate<String, String> stringRedisTemplate = mock(RedisTemplate.class);
        RedisKeyFactory keyFactory = mock(RedisKeyFactory.class);

        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOps);

        // checksum key 생성
        when(keyFactory.checksumKey("rsdt", "seoul")).thenReturn("checksum-key");
        // Redis HASH의 value가 존재하는 케이스
        when(hashOps.get("checksum-key", "std-1")).thenReturn("abc123");


        RedisMyHomeSnapshotReaderAdapter adapter =
                new RedisMyHomeSnapshotReaderAdapter(redisBytesTemplate, stringRedisTemplate, keyFactory);

        String checksum = adapter.getChecksum("rsdt", "seoul", "std-1");

        assertThat(checksum).isEqualTo("abc123");
    }

    // getChecksum이 Redis HASH(checksum)에 값이 없을 때 null을 반환하는지 테스트합니다.
    @Test
    void getChecksum_returns_null_when_absent() {

        RedisTemplate<String, byte[]> redisBytesTemplate = mock(RedisTemplate.class);
        RedisTemplate<String, String> stringRedisTemplate = mock(RedisTemplate.class);
        RedisKeyFactory keyFactory = mock(RedisKeyFactory.class);

        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOps);

        when(keyFactory.checksumKey("rsdt", "seoul")).thenReturn("checksum-key");
        // 레디스에 값이 없으면 null 반환
        when(hashOps.get("checksum-key", "std-1")).thenReturn(null);

        RedisMyHomeSnapshotReaderAdapter adapter =
                new RedisMyHomeSnapshotReaderAdapter(redisBytesTemplate, stringRedisTemplate, keyFactory);

        String checksum = adapter.getChecksum("rsdt", "seoul", "std-1");

        assertThat(checksum).isNull();
    }

    // getAllChecksums가 Redis HASH(checksum) 전체를 읽어서 Map<String,String>으로 반환하는지 테스트합니다.
    @Test
    void getAllChecksums_returns_map() {
        RedisTemplate<String, byte[]> redisBytesTemplate = mock(RedisTemplate.class);
        RedisTemplate<String, String> stringRedisTemplate = mock(RedisTemplate.class);
        RedisKeyFactory keyFactory = mock(RedisKeyFactory.class);

        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOps);

        when(keyFactory.checksumKey("rsdt", "seoul")).thenReturn("checksum-key");

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

    // getSnapshotJsonBytes가 meta.compressed=false일 때 payload를 그대로 반환하는지 테스트합니다.
    @Test
    void getSnapshotJsonBytes_not_compressed() {

        RedisTemplate<String, byte[]> redisBytesTemplate = mock(RedisTemplate.class);
        RedisTemplate<String, String> stringRedisTemplate = mock(RedisTemplate.class);
        RedisKeyFactory keyFactory = mock(RedisKeyFactory.class);

        // snapshot은 STRING(byte[]) 값 조회이므로 opsForValue() mock 필요
        ValueOperations<String, byte[]> valueOps = mock(ValueOperations.class);
        when(redisBytesTemplate.opsForValue()).thenReturn(valueOps);

        // meta는 HASH 조회이므로 opsForHash() mock 필요
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOps);

        when(keyFactory.snapshotKey("rsdt", "seoul")).thenReturn("snapshot-key");
        when(keyFactory.metaKey("rsdt", "seoul")).thenReturn("meta-key");

        byte[] payload = "hello".getBytes();
        when(valueOps.get("snapshot-key")).thenReturn(payload);

        // meta.compressed=false => gzip 해제 없이 그대로 반환
        when(hashOps.entries("meta-key")).thenReturn(Map.of("compressed", "false"));

        RedisMyHomeSnapshotReaderAdapter adapter =
                new RedisMyHomeSnapshotReaderAdapter(redisBytesTemplate, stringRedisTemplate, keyFactory);

        byte[] result = adapter.getSnapshotJsonBytes("rsdt", "seoul");

        assertThat(result).isEqualTo(payload);
    }

    // getSnapshotJsonBytes가 meta.compressed=true일 때 gzip을 해제(gunzip)해서 원본을 반환하는지 테스트합니다.
    @Test
    void getSnapshotJsonBytes_compressed() throws Exception {

        RedisTemplate<String, byte[]> redisBytesTemplate = mock(RedisTemplate.class);
        RedisTemplate<String, String> stringRedisTemplate = mock(RedisTemplate.class);
        RedisKeyFactory keyFactory = mock(RedisKeyFactory.class);

        ValueOperations<String, byte[]> valueOps = mock(ValueOperations.class);
        when(redisBytesTemplate.opsForValue()).thenReturn(valueOps);

        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOps);

        when(keyFactory.snapshotKey("rsdt", "seoul")).thenReturn("snapshot-key");
        when(keyFactory.metaKey("rsdt", "seoul")).thenReturn("meta-key");

        // 원본 바이트를 gzip으로 압축해서 Redis에 저장된 payload라고 가정
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

        // compressed=true면 gunzip 해서 원본과 동일해야 함
        assertThat(result).isEqualTo(original);
    }

    // getSnapshotJsonBytes가 Redis에 snapshot이 없을 때 null을 반환하는지 테스트합니다.
    @Test
    void getSnapshotJsonBytes_returns_null_when_absent() {

        RedisTemplate<String, byte[]> redisBytesTemplate = mock(RedisTemplate.class);
        RedisTemplate<String, String> stringRedisTemplate = mock(RedisTemplate.class);
        RedisKeyFactory keyFactory = mock(RedisKeyFactory.class);

        ValueOperations<String, byte[]> valueOps = mock(ValueOperations.class);
        when(redisBytesTemplate.opsForValue()).thenReturn(valueOps);

        when(keyFactory.snapshotKey("rsdt", "seoul")).thenReturn("snapshot-key");
        // Redis에 snapshot 자체가 없으면 null 반환
        when(valueOps.get("snapshot-key")).thenReturn(null);

        RedisMyHomeSnapshotReaderAdapter adapter =
                new RedisMyHomeSnapshotReaderAdapter(redisBytesTemplate, stringRedisTemplate, keyFactory);

        byte[] result = adapter.getSnapshotJsonBytes("rsdt", "seoul");

        assertThat(result).isNull();
    }
}
