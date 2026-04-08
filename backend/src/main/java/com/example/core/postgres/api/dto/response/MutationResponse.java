package com.example.core.postgres.api.dto.response;

import java.util.Map;

public record MutationResponse(
        long affectedRows,
        Map<String, Object> row
) {
}

