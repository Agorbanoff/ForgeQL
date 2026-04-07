package com.example.core.engine.api.dto.request;

import java.util.Map;

public record CreateRowRequest(
        Map<String, Object> values
) {
}
