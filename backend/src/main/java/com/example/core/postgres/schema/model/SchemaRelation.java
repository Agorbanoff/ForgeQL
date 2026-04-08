package com.example.core.postgres.schema.model;

import java.util.List;

public record SchemaRelation(
        String name,
        SchemaRelationType relationType,
        String sourceQualifiedName,
        String targetQualifiedName,
        List<String> sourceColumns,
        List<String> targetColumns
) {
}
