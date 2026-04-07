package com.example.core.engine.api.dto.response;

import java.util.Map;

public record AggregateResponse(
        Map<String, Object> values
) {
}
