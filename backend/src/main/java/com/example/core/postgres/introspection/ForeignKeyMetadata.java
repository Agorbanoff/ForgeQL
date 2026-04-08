package com.example.core.postgres.introspection;

import java.util.List;

public record ForeignKeyMetadata(
        String sourceSchemaName,
        String sourceTableName,
        String targetSchemaName,
        String targetTableName,
        String constraintName,
        List<String> sourceColumns,
        List<String> targetColumns
) {
}

