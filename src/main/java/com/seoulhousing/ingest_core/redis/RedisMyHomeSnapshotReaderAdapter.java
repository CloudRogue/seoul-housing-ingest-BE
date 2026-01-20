package com.seoulhousing.ingest_core.redis;


import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

@Component
public class RedisMyHomeSnapshotReaderAdapter implements MyHomeSnapshotReaderPort{
    // 소스를 마이홈으로 고정
    private static final String SOURCE = "myhome";

    private final RedisTemplate<String, byte[]> redisBytesTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    private final RedisKeyFactory keyFactory;

    public RedisMyHomeSnapshotReaderAdapter(
            @Qualifier("redisBytesTemplate") RedisTemplate<String, byte[]> redisBytesTemplate,
            @Qualifier("redisStringTemplate") RedisTemplate<String, String> redisTemplate,
            RedisKeyFactory keyFactory
    ) {
        this.redisBytesTemplate = redisBytesTemplate;
        this.redisTemplate = redisTemplate;
        this.keyFactory = keyFactory;
    }


    @Override
    public Map<String, String> getMeta(String category, String scope) {
        String metaKey = keyFactory.metaKey(SOURCE, category, scope);
        Map<Object, Object> raw = redisTemplate.opsForHash().entries(metaKey);

        Map<String, String> meta = new LinkedHashMap<>();
        raw.forEach((k, v) -> meta.put(String.valueOf(k), v == null ? null : String.valueOf(v)));
        return meta; // 없으면 빈 으로 반환하기
    }

    @Nullable
    @Override
    public String getChecksum(String category, String scope, String stdId) {
        if (stdId == null || stdId.isBlank()) {
            throw new IllegalArgumentException("stdId must not be null/blank");
        }

        String checksumKey = keyFactory.checksumKey(SOURCE, category, scope);
        Object v = redisTemplate.opsForHash().get(checksumKey, stdId);
        return (v == null) ? null : v.toString();
    }

    @Override
    public Map<String, String> getAllChecksums(String category, String scope) {
        String checksumKey = keyFactory.checksumKey(SOURCE, category, scope);
        Map<Object, Object> raw = redisTemplate.opsForHash().entries(checksumKey);

        Map<String, String> map = new LinkedHashMap<>();
        raw.forEach((k, v) -> map.put(String.valueOf(k), v == null ? null : String.valueOf(v)));
        return map; // 없으면 빈 map으로 반환
    }

    @Nullable
    @Override
    public byte[] getSnapshotJsonBytes(String category, String scope) {
        String snapshotKey = keyFactory.snapshotKey(SOURCE, category, scope);
        byte[] payload = redisBytesTemplate.opsForValue().get(snapshotKey);
        if (payload == null) return null;

        Map<String, String> meta = getMeta(category, scope);
        boolean compressed = "true".equalsIgnoreCase(meta.get("compressed"));

        return compressed ? gunzip(payload) : payload;
    }

    private static byte[] gunzip(byte[] gz) {
        try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(gz));
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            gis.transferTo(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("gunzip failed", e);
        }
    }
}
