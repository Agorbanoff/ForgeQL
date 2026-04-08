package com.example.core.postgres.introspection;

public record ColumnMetadata(
        String schemaName,
        String tableName,
        String columnName,
        String dbType,
        String typeSchemaName,
        String typeName,
        String elementTypeSchemaName,
        String elementTypeName,
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

