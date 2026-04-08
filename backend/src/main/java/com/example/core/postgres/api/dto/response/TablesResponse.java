package com.example.core.postgres.api.dto.response;

import com.example.core.postgres.schema.model.SchemaTable;

import java.util.List;

public record TablesResponse(
        List<SchemaTable> tables
) {
}
