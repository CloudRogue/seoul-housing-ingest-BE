package com.seoulhousing.ingest_core.mainserver.dto;

public record IngestResponse(
        int received,
        int created,
        int updated,
        int skipped
) {}
