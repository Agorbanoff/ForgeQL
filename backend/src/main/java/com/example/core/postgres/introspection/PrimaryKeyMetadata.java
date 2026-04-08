package com.example.core.postgres.introspection;

import java.util.List;

public record PrimaryKeyMetadata(
        String schemaName,
        String tableName,
        List<String> columns
) {
}

