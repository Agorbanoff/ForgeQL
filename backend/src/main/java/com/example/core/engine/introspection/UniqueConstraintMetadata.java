package com.example.core.engine.introspection;

import java.util.List;

public record UniqueConstraintMetadata(
        String schemaName,
        String tableName,
        String constraintName,
        List<String> columns
) {
}
