package com.example.core.postgres.api.dto.request;

import java.util.Map;

public record UpdateRowRequest(
        Map<String, Object> values
) {
}

