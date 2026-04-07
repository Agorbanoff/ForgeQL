package com.example.core.engine.api.dto.response;

import com.example.core.engine.schema.model.PostgresTableSchema;

public record TableResponse(
        PostgresTableSchema table
) {
}
