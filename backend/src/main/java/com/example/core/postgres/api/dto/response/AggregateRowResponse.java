package com.example.core.postgres.api.dto.response;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record AggregateRowResponse(
        Map<String, Object> values
) {
    public AggregateRowResponse {
        values = values == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }
}
