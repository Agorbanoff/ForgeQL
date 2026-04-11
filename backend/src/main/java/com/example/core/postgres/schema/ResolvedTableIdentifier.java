package com.example.core.postgres.schema;

import com.example.core.postgres.schema.model.SchemaTable;

public record ResolvedTableIdentifier(
        String requestedIdentifier,
        String qualifiedName,
        SchemaTable table
) {
}
