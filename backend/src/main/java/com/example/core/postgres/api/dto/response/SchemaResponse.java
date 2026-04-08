package com.example.core.postgres.api.dto.response;

import com.example.core.postgres.schema.model.GeneratedSchema;

public record SchemaResponse(
        GeneratedSchema schema
) {
}

