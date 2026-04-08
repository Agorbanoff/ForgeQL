package com.example.core.postgres.schema.model;

import java.util.List;

public record SchemaForeignKey(
        String name,
        String sourceSchema,
        String sourceTable,
        String sourceQualifiedName,
        String targetSchema,
        String targetTable,
        String targetQualifiedName,
        List<String> sourceColumns,
        List<String> targetColumns
) {
}
