package com.example.core.postgres.introspection;

import com.example.core.postgres.schema.model.PostgresTableType;

public record TableMetadata(
        String schemaName,
        String tableName,
        PostgresTableType tableType
) {
}

