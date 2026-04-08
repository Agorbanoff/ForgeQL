package com.example.core.postgres.introspection;

import com.example.common.exceptions.PostgresMetadataIntrospectionException;
import com.example.core.postgres.connection.PostgresConnectionFactory;
import com.example.core.postgres.connection.PostgresConnectionValidator;
import com.example.core.postgres.connection.PostgresRuntimeConnectionDefinition;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Service
public class PostgresMetadataIntrospector {

    private static final String CURRENT_SCHEMA_SQL = "SELECT current_schema()";

    private final PostgresConnectionFactory connectionFactory;
    private final PostgresConnectionValidator connectionValidator;
    private final TableMetadataLoader tableMetadataLoader;
    private final ColumnMetadataLoader columnMetadataLoader;
    private final PrimaryKeyMetadataLoader primaryKeyMetadataLoader;
    private final UniqueConstraintMetadataLoader uniqueConstraintMetadataLoader;
    private final ForeignKeyMetadataLoader foreignKeyMetadataLoader;
    private final EnumMetadataLoader enumMetadataLoader;

    public PostgresMetadataIntrospector(
            PostgresConnectionFactory connectionFactory,
            PostgresConnectionValidator connectionValidator,
            TableMetadataLoader tableMetadataLoader,
            ColumnMetadataLoader columnMetadataLoader,
            PrimaryKeyMetadataLoader primaryKeyMetadataLoader,
            UniqueConstraintMetadataLoader uniqueConstraintMetadataLoader,
            ForeignKeyMetadataLoader foreignKeyMetadataLoader,
            EnumMetadataLoader enumMetadataLoader
    ) {
        this.connectionFactory = connectionFactory;
        this.connectionValidator = connectionValidator;
        this.tableMetadataLoader = tableMetadataLoader;
        this.columnMetadataLoader = columnMetadataLoader;
        this.primaryKeyMetadataLoader = primaryKeyMetadataLoader;
        this.uniqueConstraintMetadataLoader = uniqueConstraintMetadataLoader;
        this.foreignKeyMetadataLoader = foreignKeyMetadataLoader;
        this.enumMetadataLoader = enumMetadataLoader;
    }

    public PostgresIntrospectionResult introspect(PostgresRuntimeConnectionDefinition definition) {
        try (Connection connection = connectionFactory.openConnection(definition)) {
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            connectionValidator.validateProductName(databaseMetaData.getDatabaseProductName());

            return new PostgresIntrospectionResult(
                    databaseMetaData.getDatabaseProductVersion(),
                    resolveDefaultSchema(connection, definition),
                    tableMetadataLoader.load(connection),
                    columnMetadataLoader.load(connection),
                    primaryKeyMetadataLoader.load(connection),
                    uniqueConstraintMetadataLoader.load(connection),
                    foreignKeyMetadataLoader.load(connection),
                    enumMetadataLoader.load(connection)
            );
        } catch (SQLException e) {
            throw new PostgresMetadataIntrospectionException("Failed to introspect PostgreSQL metadata", e);
        }
    }

    private String resolveDefaultSchema(Connection connection, PostgresRuntimeConnectionDefinition definition) throws SQLException {
        String schema = connection.getSchema();
        if (schema != null && !schema.isBlank()) {
            return schema;
        }

        try (PreparedStatement statement = connection.prepareStatement(CURRENT_SCHEMA_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                schema = resultSet.getString(1);
            }
        }

        if (schema != null && !schema.isBlank()) {
            return schema;
        }

        return definition.schemaName();
    }
}

