package com.example.core.engine.api.dto.request;

import java.util.Map;

public record UpdateRowRequest(
        Map<String, Object> values
) {
}
