package com.example.core.engine.introspection;

import com.example.core.engine.schema.model.PostgresTableType;

public record TableMetadata(
        String schemaName,
        String tableName,
        PostgresTableType tableType
) {
}
