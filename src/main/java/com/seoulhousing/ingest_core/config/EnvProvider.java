package com.seoulhousing.ingest_core.config;


import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

//실행 환경을 판별하는 컴포넌트(local/dev/prod) Redis 키 prefix에 env를 넣기 위해 필요함
@Component
public class EnvProvider {

    private final Environment environment;

    public EnvProvider(Environment environment) {
        this.environment = environment;
    }

    // 현재 env 가 뭐인지 반환해주는 메서드
    public String envKey(){
        Set<String> actives = activeProfileSet();

        //뭐가 켜졌나 확인
        boolean prod = actives.contains("prod");
        boolean dev = actives.contains("dev");
        boolean local = actives.contains("local");

        int count = (prod ? 1 : 0) + (dev ? 1 : 0) + (local ? 1 : 0);

        if (count == 0) return "local";

        // 2개 이상 켜지면 에러 발생 시켜주기
        if (count > 1) {
            throw new IllegalStateException("프로파일은 prod/dev/local 중 하나만 활성화하세요. activeProfiles=" + actives);
        }

        if (prod) return "prod";
        if (dev)  return "dev";
        return "local";
    }

    //활성 프로파일을 정규화해서 set으로 저장
    private Set<String> activeProfileSet(){
        return Arrays.stream(environment.getActiveProfiles())
                .filter(p -> p != null && !p.isBlank()) // 공백 제거
                .map(p -> p.trim().toLowerCase(Locale.ROOT)) // 소문자 통일
                .collect(Collectors.toSet());
    }
}
