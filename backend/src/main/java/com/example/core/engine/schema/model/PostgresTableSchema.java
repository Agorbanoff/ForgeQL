package com.example.core.engine.schema.model;

import java.util.List;

public record PostgresTableSchema(
        String name,
        String schema,
        String qualifiedName,
        PostgresTableType tableType,
        PostgresPrimaryKeySchema primaryKey,
        List<PostgresUniqueConstraintSchema> uniqueConstraints,
        List<PostgresColumnSchema> columns,
        List<PostgresRelationSchema> relations,
        PostgresTableCapabilities capabilities
) {
}
