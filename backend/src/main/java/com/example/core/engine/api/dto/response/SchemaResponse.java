package com.example.core.engine.api.dto.response;

import com.example.core.engine.schema.model.PostgresSchemaSnapshot;

public record SchemaResponse(
        PostgresSchemaSnapshot schema
) {
}
