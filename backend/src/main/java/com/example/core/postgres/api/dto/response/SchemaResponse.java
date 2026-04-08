package com.example.core.postgres.api.dto.response;

import com.example.core.postgres.schema.model.PostgresSchemaSnapshot;

public record SchemaResponse(
        PostgresSchemaSnapshot schema
) {
}

