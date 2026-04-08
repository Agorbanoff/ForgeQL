package com.example.core.postgres.introspection;

import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Component
public class TableMetadataLoader {

    private static final String LOAD_TABLES_SQL = """
            SELECT
                n.nspname AS schema_name,
                c.relname AS table_name,
                CASE c.relkind
                    WHEN 'v' THEN 'VIEW'
                    WHEN 'm' THEN 'MATERIALIZED_VIEW'
                    ELSE 'TABLE'
                END AS table_type
            FROM pg_catalog.pg_class c
            JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
            WHERE c.relkind IN ('r', 'p', 'v', 'm')
              AND """
            + PostgresIntrospectionSupport.nonSystemSchemaPredicate("n.nspname")
            + """
            ORDER BY n.nspname, c.relname
            """;

    public List<TableMetadata> load(Connection connection) throws SQLException {
        List<TableMetadata> tables = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(LOAD_TABLES_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                tables.add(new TableMetadata(
                        PostgresIntrospectionSupport.requireIdentifier(resultSet, "schema_name"),
                        PostgresIntrospectionSupport.requireIdentifier(resultSet, "table_name"),
                        parseTableType(PostgresIntrospectionSupport.requireIdentifier(resultSet, "table_type"))
                ));
            }
        }

        return tables;
    }

    private PostgresRawTableType parseTableType(String rawValue) throws SQLException {
        try {
            return PostgresRawTableType.valueOf(rawValue);
        } catch (IllegalArgumentException exception) {
            throw new SQLException("PostgreSQL metadata query returned unsupported table_type " + rawValue, exception);
        }
    }
}

