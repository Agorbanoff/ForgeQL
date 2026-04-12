package com.example.core.postgres.api.dto.response;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record CreateRowResponse(
        long affectedRows,
        Object createdIdentity,
        Map<String, Object> row
) {
    public CreateRowResponse {
        row = row == null
                ? null
                : Collections.unmodifiableMap(new LinkedHashMap<>(row));
    }
}
