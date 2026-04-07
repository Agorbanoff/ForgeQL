package com.example.core.engine.introspection;

import com.example.core.engine.connection.PostgresRuntimeConnectionDefinition;
import org.springframework.stereotype.Service;

@Service
public class PostgresMetadataIntrospector {

    private final TableMetadataLoader tableMetadataLoader;
    private final ColumnMetadataLoader columnMetadataLoader;
    private final PrimaryKeyMetadataLoader primaryKeyMetadataLoader;
    private final UniqueConstraintMetadataLoader uniqueConstraintMetadataLoader;
    private final ForeignKeyMetadataLoader foreignKeyMetadataLoader;
    private final EnumMetadataLoader enumMetadataLoader;

    public PostgresMetadataIntrospector(
            TableMetadataLoader tableMetadataLoader,
            ColumnMetadataLoader columnMetadataLoader,
            PrimaryKeyMetadataLoader primaryKeyMetadataLoader,
            UniqueConstraintMetadataLoader uniqueConstraintMetadataLoader,
            ForeignKeyMetadataLoader foreignKeyMetadataLoader,
            EnumMetadataLoader enumMetadataLoader
    ) {
        this.tableMetadataLoader = tableMetadataLoader;
        this.columnMetadataLoader = columnMetadataLoader;
        this.primaryKeyMetadataLoader = primaryKeyMetadataLoader;
        this.uniqueConstraintMetadataLoader = uniqueConstraintMetadataLoader;
        this.foreignKeyMetadataLoader = foreignKeyMetadataLoader;
        this.enumMetadataLoader = enumMetadataLoader;
    }

    public PostgresIntrospectionResult introspect(PostgresRuntimeConnectionDefinition definition) {
        throw new UnsupportedOperationException("Metadata introspection is not implemented yet for datasource " + definition.datasourceId());
    }
}
