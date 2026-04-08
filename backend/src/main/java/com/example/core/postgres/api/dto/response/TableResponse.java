package com.example.core.postgres.api.dto.response;

import com.example.core.postgres.schema.model.SchemaTable;

public record TableResponse(
        SchemaTable table
) {
}

