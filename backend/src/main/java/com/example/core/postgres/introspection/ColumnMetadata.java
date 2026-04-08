package com.example.core.postgres.introspection;

public record ColumnMetadata(
        String schemaName,
        String tableName,
        String columnName,
        String dbType,
        boolean nullable,
        String defaultValue,
        int ordinalPosition,
        boolean identity,
        boolean generated,
        Integer precision,
        Integer scale,
        Integer length
) {
}

