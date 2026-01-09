package com.seoulhousing.ingest_core.announcement.service;

import com.seoulhousing.ingest_core.announcement.port.SeenStdIdReaderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DefaultNewStdIdDetector implements NewStdIdDetector {

    private final SeenStdIdReaderPort seenStdIdReaderPort;

    @Override
    public List<String> detectNewStdIds(String source, String category, String scope, List<String> currentStdIds) {

        if (currentStdIds == null || currentStdIds.isEmpty()) {
            return List.of();
        }

        // Redis에 저장된 이미 본 stdId
        Set<String> seen = seenStdIdReaderPort.getSeenStdIds(source, category, scope);
        if (seen == null) seen = Set.of();


        List<String> newOnes = new ArrayList<>();
        for (String raw : currentStdIds) {
            if (raw == null) continue;
            String stdId = raw.trim();
            if (stdId.isEmpty()) continue;

            if (!seen.contains(stdId)) {
                newOnes.add(stdId);
            }
        }

        return List.copyOf(newOnes);


    }
}
