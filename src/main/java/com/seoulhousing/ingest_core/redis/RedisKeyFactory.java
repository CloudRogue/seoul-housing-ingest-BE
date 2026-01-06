package com.seoulhousing.ingest_core.redis;

import com.seoulhousing.ingest_core.config.EnvProvider;
import org.springframework.stereotype.Component;

import java.util.Locale;

// 레디스 키 문자열을 규칙대로 만들어주는 공장 클래스
@Component
public class RedisKeyFactory {
    //env값 주입
    private final EnvProvider envProvider;

    //키버전과 소스 고정
    private static final String SOURCE = "myhome";
    private static final String VERSION = "v1";

    public RedisKeyFactory(EnvProvider envProvider) {
        this.envProvider = envProvider;
    }

    // 스냅샷 체크섬,메타,락 키 생성
    public String snapshotKey(String category, String scope) {
        return snapshotKey(category, scope, VERSION);
    }
    public String checksumKey(String category, String scope) {
        return checksumKey(category, scope, VERSION);
    }
    public String metaKey(String category, String scope) {
        return metaKey(category, scope, VERSION);
    }
    public String lockKey(String category, String scope) {
        return lockKey(category, scope, VERSION);
    }

    // 버전지정 가능하게 키생성 왜냐하면 다른버전과 혼용될수도있으니
    public String snapshotKey(String category, String scope, String version) {
        return basePrefix() + ":" + SOURCE + ":" + norm(category) + ":" + norm(scope) + ":snapshot:" + norm(version);
    }
    public String checksumKey(String category, String scope, String version) {
        return basePrefix() + ":" + SOURCE + ":" + norm(category) + ":" + norm(scope) + ":checksum:" + norm(version);
    }
    public String metaKey(String category, String scope, String version) {
        return basePrefix() + ":" + SOURCE + ":" + norm(category) + ":" + norm(scope) + ":meta:" + norm(version);
    }
    public String lockKey(String category, String scope, String version) {
        return basePrefix() + ":" + SOURCE + ":" + norm(category) + ":" + norm(scope) + ":lock:" + norm(version);
    }

    // seoulhousing:{env}:ingest prefix 생성
    private String basePrefix() {
        return "seoulhousing:" + envProvider.envKey() + ":ingest";
    }

    // 값 정규화 공백 및 대소문자 방지용으로 설정
    private String norm(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("redis key 파라미터가 비어있음");
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }
}
