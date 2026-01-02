package com.seoulhousing.ingest_core.external.myhome.client;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.regex.Pattern;

@Component
public class MyHomeLogSanitizer {

    private static final Pattern SERVICE_KEY_PATTERN =
            Pattern.compile("(serviceKey=)([^&]+)");

    public String toSafeLogUri(URI uri) {
        if (uri == null) return "null";

        String path = uri.getRawPath() == null ? "" : uri.getRawPath();
        String query = uri.getRawQuery();

        String raw = (query == null || query.isBlank())
                ? path
                : path + "?" + query;

        return maskServiceKey(raw);
    }

    public String maskServiceKey(String raw) {
        if (raw == null) return "null";
        return SERVICE_KEY_PATTERN.matcher(raw).replaceAll("$1****");
    }
}
