package com.seoulhousing.ingest_core.mainserver.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

// 앱 부팅시 1회 실행후 종료시키는 러너
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ingest.oneshot.enabled", havingValue = "true", matchIfMissing = true)
public class OneShotJobRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OneShotJobRunner.class);

    private final IngestJobService ingestJobService;
    private final ApplicationContext ctx; // 종료를 위한 컨텍스트

    @Override
    public void run(ApplicationArguments args) {
        int exitCode = 0; // 성공=0, 실패=1

        try {
            // 작업 한번 실행
            ingestJobService.runOnce();

            // 정상 종료 코드
            exitCode = 0;
            log.info("[RUNNER] job success. exitCode={}", exitCode);

        } catch (Exception e) {
            // 예외 발생 시 실패 종료 코드
            exitCode = 1;
            log.error("[RUNNER] job failed. exitCode={}, ex={}", exitCode, e.getClass().getSimpleName(), e);

        } finally {

            //항상 실행되는 블록
            final int finalExitCode = exitCode;
            int code = SpringApplication.exit(ctx, () -> finalExitCode);
            System.exit(code);
        }
    }
}
