package com.example.core.postgres.api.dto.response;

public record RowsPageResponse(
        int returnedCount,
        Integer limit,
        Integer offset
) {
}
