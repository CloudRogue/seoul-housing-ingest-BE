package com.seoulhousing.ingest_core.announcement.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.List;


@Getter
@RequiredArgsConstructor
@ToString
public class ChangeDetectionResult {

    private final String source;
    private final String category;
    private final String scope;

    private final List<String> newStdIds;
    private final List<String> missingStdIds;

    private final int currentCount;
    private final int seenCount;
}
