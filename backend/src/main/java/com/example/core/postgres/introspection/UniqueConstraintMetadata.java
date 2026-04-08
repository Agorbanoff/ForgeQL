package com.example.core.postgres.introspection;

import java.util.List;

public record UniqueConstraintMetadata(
        String schemaName,
        String tableName,
        String constraintName,
        List<String> columns
) {
}

