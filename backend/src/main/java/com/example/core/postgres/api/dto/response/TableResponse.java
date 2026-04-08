package com.example.core.postgres.api.dto.response;

import com.example.core.postgres.schema.model.PostgresTableSchema;

public record TableResponse(
        PostgresTableSchema table
) {
}

