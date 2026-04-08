package com.example.core.postgres.api.dto.response;

import java.util.Map;

public record AggregateResponse(
        Map<String, Object> values
) {
}

