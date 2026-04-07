package com.example.core.engine.api.dto.response;

import java.util.List;
import java.util.Map;

public record RowsResponse(
        List<Map<String, Object>> rows
) {
}
