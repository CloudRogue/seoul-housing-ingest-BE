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


    @Override
    public List<String> detect(Set<String> seenStdIds, List<String> currentStdIds) {

        if (currentStdIds == null || currentStdIds.isEmpty()) {
            return List.of();
        }

        Set<String> seen = (seenStdIds == null) ? Set.of() : seenStdIds;


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
