package com.seoulhousing.ingest_core.mainserver.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
//메인 서버로 보내는 요청
public record AnnouncementIngestRequest(
        @NotBlank(message = "category는 필수입니다.")
        String category,

        @NotEmpty(message = "items는 비어있을 수 없습니다.")
        @Valid
        List<AnnouncementIngestItem> items
) { }
