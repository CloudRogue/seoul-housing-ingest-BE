package com.seoulhousing.ingest_core.announcement.adapter;

import com.seoulhousing.ingest_core.announcement.port.SeenStdIdReaderPort;
import com.seoulhousing.ingest_core.config.EnvProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RedisSeenStdIdReaderAdapter  implements SeenStdIdReaderPort {
    @Qualifier("redisStringTemplate")
    private final RedisTemplate<String, String> redisStringTemplate;

    private final EnvProvider envProvider;

    @Override
    public Set<String> getSeenStdIds(String source, String category, String scope) {
        String key = seenKey(source, category, scope);

        // Redis 전부 조회
        Set<String> members = redisStringTemplate.opsForSet().members(key);

        // Redis가 null 줄 수도 있으니 방어
        if (members == null) return Collections.emptySet();
        return members;
    }

    // seen stdId set 전용키 생성
    private String seenKey(String source, String category, String scope) {
        return "seoulhousing:" + norm(envProvider.envKey())
                + ":seen:" + norm(source)
                + ":" + norm(category)
                + ":" + norm(scope);
    }

    private static String norm(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("seen key 파라미터가 비어있음");
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }
}
