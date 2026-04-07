package com.example.core.engine.api.dto.request;

import java.util.List;
import java.util.Map;

public record AggregateRequest(
        List<String> groupBy,
        Map<String, String> operations,
        Map<String, Object> filter
) {
}
