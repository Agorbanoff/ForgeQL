package com.example.core.postgres.schema.model;

import java.util.List;

public record PostgresRelationSchema(
        String name,
        String relationType,
        String sourceQualifiedName,
        String targetQualifiedName,
        List<String> sourceColumns,
        List<String> targetColumns
) {
}

