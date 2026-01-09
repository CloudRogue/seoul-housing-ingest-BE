package com.seoulhousing.ingest_core.announcement.service;

import com.seoulhousing.ingest_core.announcement.dto.ChangeDetectionResult;
import com.seoulhousing.ingest_core.announcement.port.SeenStdIdReaderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DefaultAnnouncementChangeDetectionService  implements AnnouncementChangeDetectionService{

    private final NewStdIdDetector newStdIdDetector;
    private final MissingStdIdDetector missingStdIdDetector;
    private final SeenStdIdReaderPort seenStdIdReaderPort;

    @Override
    public ChangeDetectionResult detect(
            String source,
            String category,
            String scope,
            List<String> currentStdIds
    ) {

        // Redis에 저장된 기존 stdId들
        Set<String> seen = seenStdIdReaderPort.getSeenStdIds(source, category, scope);
        if (seen == null) seen = Set.of();

        // 신규 stdId 감지
        List<String> newStdIds =
                newStdIdDetector.detectNewStdIds(source, category, scope, currentStdIds);

        // 누락 stdId 감지
        List<String> missingStdIds =
                missingStdIdDetector.detectMissingStdIds(source, category, scope, currentStdIds);

        // 결과 요약 DTO 생성
        return new ChangeDetectionResult(
                source,
                category,
                scope,
                newStdIds,
                missingStdIds,
                currentStdIds == null ? 0 : currentStdIds.size(),
                seen.size()
        );
    }
}
