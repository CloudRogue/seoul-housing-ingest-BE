package com.seoulhousing.ingest_core.redis;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Tag("it")
class RedisSnapshotReadersIntegrationTest {

    private static final String MYHOME_SOURCE = "myhome";
    private static final String SH_SOURCE = "sh";

    @Autowired RedisKeyFactory keyFactory;

    @Autowired RedisTemplate<String, String> redisStringTemplate; // meta/checksum hash
    @Autowired RedisTemplate<String, byte[]> redisBytesTemplate;  // snapshot value

    @Autowired RedisMyHomeSnapshotReaderAdapter myHomeReader;
    @Autowired RedisShRssSnapshotReaderAdapter shRssReader;

    @AfterEach
    void cleanup() {
        deleteKeys(MYHOME_SOURCE, "rsdt", "seoul");
        deleteKeys(SH_SOURCE, "announcement", "all");
    }

    private void deleteKeys(String source, String category, String scope) {
        String metaKey = keyFactory.metaKey(source, category, scope);
        String snapshotKey = keyFactory.snapshotKey(source, category, scope);
        String checksumKey = keyFactory.checksumKey(source, category, scope);

        redisStringTemplate.delete(metaKey);
        redisBytesTemplate.delete(snapshotKey);
        redisStringTemplate.delete(checksumKey); // sh는 안써도 상관없음
    }

    // ---------------- MyHome ----------------

    @Test
    void myhome_reads_meta_checksum_snapshot_plain() {
        String category = "rsdt";
        String scope = "seoul";

        String metaKey = keyFactory.metaKey(MYHOME_SOURCE, category, scope);
        String snapshotKey = keyFactory.snapshotKey(MYHOME_SOURCE, category, scope);
        String checksumKey = keyFactory.checksumKey(MYHOME_SOURCE, category, scope);

        redisStringTemplate.opsForHash().put(metaKey, "compressed", "false");
        redisStringTemplate.opsForHash().put(metaKey, "count", "2");

        redisStringTemplate.opsForHash().put(checksumKey, "std-1", "hash-1");
        redisStringTemplate.opsForHash().put(checksumKey, "std-2", "hash-2");

        byte[] payload = "{\"hello\":\"world\"}".getBytes(StandardCharsets.UTF_8);
        redisBytesTemplate.opsForValue().set(snapshotKey, payload);

        Map<String, String> meta = myHomeReader.getMeta(category, scope);
        assertThat(meta).containsEntry("compressed", "false");
        assertThat(meta).containsEntry("count", "2");

        assertThat(myHomeReader.getChecksum(category, scope, "std-1")).isEqualTo("hash-1");

        Map<String, String> all = myHomeReader.getAllChecksums(category, scope);
        assertThat(all).containsEntry("std-1", "hash-1");
        assertThat(all).containsEntry("std-2", "hash-2");

        assertThat(myHomeReader.getSnapshotJsonBytes(category, scope)).isEqualTo(payload);
    }

    @Test
    void myhome_reads_snapshot_gzipped_when_meta_compressed_true() throws Exception {
        String category = "rsdt";
        String scope = "seoul";

        String metaKey = keyFactory.metaKey(MYHOME_SOURCE, category, scope);
        String snapshotKey = keyFactory.snapshotKey(MYHOME_SOURCE, category, scope);

        redisStringTemplate.opsForHash().put(metaKey, "compressed", "true");

        byte[] original = "{\"gzip\":true}".getBytes(StandardCharsets.UTF_8);
        redisBytesTemplate.opsForValue().set(snapshotKey, gzip(original));

        assertThat(myHomeReader.getSnapshotJsonBytes(category, scope)).isEqualTo(original);
    }

    @Test
    void myhome_returns_null_when_snapshot_missing() {
        assertThat(myHomeReader.getSnapshotJsonBytes("rsdt", "seoul")).isNull();
    }

    // ---------------- SH RSS ----------------

    @Test
    void sh_reads_meta_and_snapshot_plain() {
        String category = "announcement";
        String scope = "all";

        String metaKey = keyFactory.metaKey(SH_SOURCE, category, scope);
        String snapshotKey = keyFactory.snapshotKey(SH_SOURCE, category, scope);

        redisStringTemplate.opsForHash().put(metaKey, "compressed", "false");
        redisStringTemplate.opsForHash().put(metaKey, "charset", "euc-kr");

        byte[] rssBytes = "<rss>...</rss>".getBytes(StandardCharsets.UTF_8);
        redisBytesTemplate.opsForValue().set(snapshotKey, rssBytes);

        assertThat(shRssReader.getMeta(category, scope)).containsEntry("compressed", "false");
        assertThat(shRssReader.getMeta(category, scope)).containsEntry("charset", "euc-kr");
        assertThat(shRssReader.getSnapshotBytes(category, scope)).isEqualTo(rssBytes);
    }

    @Test
    void sh_reads_snapshot_gzipped_when_meta_compressed_true() throws Exception {
        String category = "announcement";
        String scope = "all";

        String metaKey = keyFactory.metaKey(SH_SOURCE, category, scope);
        String snapshotKey = keyFactory.snapshotKey(SH_SOURCE, category, scope);

        redisStringTemplate.opsForHash().put(metaKey, "compressed", "true");

        byte[] original = "<rss><item>gzip</item></rss>".getBytes(StandardCharsets.UTF_8);
        redisBytesTemplate.opsForValue().set(snapshotKey, gzip(original));

        assertThat(shRssReader.getSnapshotBytes(category, scope)).isEqualTo(original);
    }

    @Test
    void sh_returns_null_when_snapshot_missing() {
        assertThat(shRssReader.getSnapshotBytes("announcement", "all")).isNull();
    }

    private static byte[] gzip(byte[] original) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gos = new GZIPOutputStream(baos)) {
            gos.write(original);
        }
        return baos.toByteArray();
    }
}
