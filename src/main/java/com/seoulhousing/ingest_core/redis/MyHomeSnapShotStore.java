package com.seoulhousing.ingest_core.redis;

import com.seoulhousing.ingest_core.external.myhome.dto.MyHomeItemDto;
import com.seoulhousing.ingest_core.redis.config.IngestSnapShotProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.zip.GZIPOutputStream;

@Service
public class MyHomeSnapShotStore {

    private final RedisTemplate<String, byte[]> redisBytesTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    private final RedisKeyFactory keyFactory;
    private final ObjectMapper objectMapper;

    // yml에서 주입 받은값을 사용
    private final Duration ttl;
    private final int gzipThreshold;

    public MyHomeSnapShotStore(
            @Qualifier("redisBytesTemplate") RedisTemplate<String, byte[]> redisBytesTemplate,
            @Qualifier("redisTemplate") RedisTemplate<String, String> redisTemplate,
            RedisKeyFactory keyFactory,
            ObjectMapper objectMapper,
            IngestSnapShotProperties props
    ) {
        this.redisBytesTemplate = redisBytesTemplate;
        this.redisTemplate = redisTemplate;
        this.keyFactory = keyFactory;
        this.objectMapper = objectMapper;
        this.ttl = Duration.ofDays(props.getTtlDays());
        this.gzipThreshold = props.getGzipThresholdBytes();
    }


    // 카테고리와 스코프 단위로 스냅샷 + 체크섬 + 메타를 저장한다
    public void save(String category, String scope, List<MyHomeItemDto> items) {
        if (items == null) throw new IllegalArgumentException("items must not be null");

        // items를 복사해서 리스트로 변경한후 stdId 기주으로 정렬
        List<MyHomeItemDto> sorted = new ArrayList<>(items);
        sorted.sort(Comparator.comparing(i -> stdId(category, i)));

        // json byte로 직렬화 한뒤 크기에따라 압축
        byte[] jsonBytes = toJsonBytes(sorted);
        boolean compressed = jsonBytes.length >= gzipThreshold;
        byte[] payload = compressed ? gzip(jsonBytes) : jsonBytes;

        // 종류에 맞는 레디스 키생성
        String snapshotKey = keyFactory.snapshotKey(category, scope);
        String checksumKey = keyFactory.checksumKey(category, scope);
        String metaKey = keyFactory.metaKey(category, scope);

        // snapshot 저장 + TTL
        redisBytesTemplate.opsForValue().set(snapshotKey, payload, ttl);

        // checksum 저장 전에 기존키를 삭제한다
        redisTemplate.delete(checksumKey);

        //stdid별로 체크섬계산해서 해쉬에 저장
        for (MyHomeItemDto item : sorted) {
            String id = stdId(category, item);
            String hash = sha256Hex(toJsonBytes(item));
            redisTemplate.opsForHash().put(checksumKey, id, hash);
        }
        redisTemplate.expire(checksumKey, ttl);

        // 메타도 기존키삭제
        redisTemplate.delete(metaKey);

        // 메타 내용을 맵으로 만들기
        Map<String, String> meta = new LinkedHashMap<>();
        meta.put("generatedAt", Instant.now().toString());
        meta.put("count", String.valueOf(sorted.size()));
        meta.put("schemaVersion", "v1");
        meta.put("serializer", "jackson");
        meta.put("compressed", String.valueOf(compressed));
        meta.put("compression", compressed ? "gzip" : "none");
        meta.put("payloadBytes", String.valueOf(payload.length));

        // 한꺼번에 저장하고 TTL 적용하기
        redisTemplate.opsForHash().putAll(metaKey, meta);
        redisTemplate.expire(metaKey, ttl);
    }

    //stdId 생성규칙
    private String stdId(String category, MyHomeItemDto item) {
        String pblancId = safe(item.getPblancId());
        String houseSn = safe(item.getHouseSn());
        return "myhome:" + category + ":" + pblancId + ":" + houseSn;
    }

    //비정상입력이면 "NA"로 처리
    private static String safe(String v) {
        if (v == null || v.isBlank()) return "NA";
        return v.trim();
    }

    //json byte로 변환 실패시 에러발생
    private byte[] toJsonBytes(Object obj) {
        try {
            return objectMapper.writeValueAsBytes(obj);
        } catch (Exception e) {
            throw new IllegalStateException("json serialize failed", e);
        }
    }

    //체크섬 저장값 만들기
    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes);
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("sha256 failed", e);
        }
    }

    //gzip압축
    private static byte[] gzip(byte[] raw) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gos = new GZIPOutputStream(baos)) {
            gos.write(raw);
            gos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("gzip failed", e);
        }
    }
}
