package com.example.core.postgres.api.dto.request;

import java.util.Map;

public record CreateRowRequest(
        Map<String, Object> values
) {
}

