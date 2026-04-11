package com.example.core.postgres.api.dto.response;

import java.util.List;

public record AggregateResponse(
        List<AggregateRowResponse> rows
) {
    public AggregateResponse {
        rows = rows == null ? List.of() : List.copyOf(rows);
    }
}

