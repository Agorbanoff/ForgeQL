package com.example.core.postgres.introspection;

import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PrimaryKeyMetadataLoader {

    private static final String LOAD_PRIMARY_KEYS_SQL = """
            SELECT
                n.nspname AS schema_name,
                c.relname AS table_name,
                a.attname AS column_name,
                key_columns.ordinality AS column_position
            FROM pg_catalog.pg_constraint con
            JOIN pg_catalog.pg_class c ON c.oid = con.conrelid
            JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
            JOIN unnest(con.conkey) WITH ORDINALITY AS key_columns(attnum, ordinality) ON true
            JOIN pg_catalog.pg_attribute a ON a.attrelid = c.oid AND a.attnum = key_columns.attnum
            WHERE con.contype = 'p'
              AND """
            + PostgresIntrospectionSupport.nonSystemSchemaPredicate("n.nspname")
            + """
            ORDER BY n.nspname, c.relname, key_columns.ordinality
            """;

    public List<PrimaryKeyMetadata> load(Connection connection) throws SQLException {
        Map<TableKey, List<String>> primaryKeys = new LinkedHashMap<>();

        try (PreparedStatement statement = connection.prepareStatement(LOAD_PRIMARY_KEYS_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                TableKey tableKey = new TableKey(
                        PostgresIntrospectionSupport.requireIdentifier(resultSet, "schema_name"),
                        PostgresIntrospectionSupport.requireIdentifier(resultSet, "table_name")
                );
                primaryKeys.computeIfAbsent(tableKey, ignored -> new ArrayList<>())
                        .add(PostgresIntrospectionSupport.requireIdentifier(resultSet, "column_name"));
            }
        }

        List<PrimaryKeyMetadata> results = new ArrayList<>();
        for (Map.Entry<TableKey, List<String>> entry : primaryKeys.entrySet()) {
            results.add(new PrimaryKeyMetadata(
                    entry.getKey().schemaName(),
                    entry.getKey().tableName(),
                    List.copyOf(entry.getValue())
            ));
        }

        return results;
    }

    private record TableKey(String schemaName, String tableName) {
    }
}

