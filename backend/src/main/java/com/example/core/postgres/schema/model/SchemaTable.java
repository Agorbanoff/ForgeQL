package com.example.core.postgres.schema.model;

import java.util.List;

public record SchemaTable(
        String name,
        String schema,
        String qualifiedName,
        SchemaTableType tableType,
        SchemaPrimaryKey primaryKey,
        List<SchemaUniqueConstraint> uniqueConstraints,
        List<SchemaForeignKey> foreignKeys,
        List<SchemaRelation> relations,
        List<SchemaColumn> columns,
        SchemaTableCapabilities capabilities
) {
}
