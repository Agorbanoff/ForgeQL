package com.example.core.postgres.api.dto.response;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record UpdateRowResponse(
        long affectedRows,
        Map<String, Object> row
) {
    public UpdateRowResponse {
        row = row == null
                ? null
                : Collections.unmodifiableMap(new LinkedHashMap<>(row));
    }
}
