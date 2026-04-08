package com.example.core.postgres.introspection;

public record TableMetadata(
        String schemaName,
        String tableName,
        PostgresRawTableType tableType
) {
}

