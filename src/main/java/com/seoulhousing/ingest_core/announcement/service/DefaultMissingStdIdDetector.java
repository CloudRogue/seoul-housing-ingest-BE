package com.seoulhousing.ingest_core.announcement.service;

import com.seoulhousing.ingest_core.announcement.port.SeenStdIdReaderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

// 기본 누락 후보 감지기
@Service
@RequiredArgsConstructor
public class DefaultMissingStdIdDetector implements MissingStdIdDetector{

    private final SeenStdIdReaderPort seenStdIdReaderPort;

    @Override
    public List<String> detectMissingStdIds(String source, String category, String scope, List<String> currentStdIds) {
       //이미 저장된 stdId
        Set<String> seen = seenStdIdReaderPort.getSeenStdIds(source, category, scope);
        if (seen == null || seen.isEmpty()) {
            return List.of();
        }

        //신규 stdIds를 정규화해주기
        Set<String> current = normalizeToSet(currentStdIds);

        List<String> missing = new ArrayList<>();
        for (String s : seen) {
            if (s == null) continue;
            String stdId = s.trim();
            if (stdId.isEmpty()) continue;

            if (!current.contains(stdId)) {
                missing.add(stdId);
            }
        }


        return List.copyOf(missing);
    }

    private static Set<String> normalizeToSet(List<String> rawList) {
        if (rawList == null || rawList.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> set = new HashSet<>();
        for (String raw : rawList) {
            if (raw == null) continue;
            String v = raw.trim();
            if (v.isEmpty()) continue;
            set.add(v);
        }
        return set;
    }
}
