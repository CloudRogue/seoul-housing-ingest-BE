package com.seoulhousing.ingest_core.external.myhome.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class MyHomeListResponse {

    @JsonProperty("response")
    private Response response;

    public List<MyHomeItemDto> itemsOrEmpty() {
        if (response == null || response.body == null || response.body.item == null) return List.of();
        return response.body.item;
    }

    public String totalCountOrNull() {
        if (response == null || response.body == null) return null;
        return response.body.totalCount;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Response {
        private Header header;
        private Body body;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Header {
        private String resultCode;
        private String resultMsg;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {
        private String totalCount;
        private String numOfRows;
        private String pageNo;

        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        private List<MyHomeItemDto> item;
    }
}
