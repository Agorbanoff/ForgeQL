package com.example.core.postgres.introspection;

import java.util.List;

public record PostgresIntrospectionResult(
        String serverVersion,
        String defaultSchema,
        List<TableMetadata> tables,
        List<ColumnMetadata> columns,
        List<PrimaryKeyMetadata> primaryKeys,
        List<UniqueConstraintMetadata> uniqueConstraints,
        List<ForeignKeyMetadata> foreignKeys,
        List<EnumMetadata> enums
) {
}

