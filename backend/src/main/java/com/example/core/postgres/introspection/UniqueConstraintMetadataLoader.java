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
public class UniqueConstraintMetadataLoader {

    private static final String LOAD_UNIQUE_CONSTRAINTS_SQL = """
            SELECT
                n.nspname AS schema_name,
                c.relname AS table_name,
                con.conname AS constraint_name,
                a.attname AS column_name,
                key_columns.ordinality AS column_position
            FROM pg_catalog.pg_constraint con
            JOIN pg_catalog.pg_class c ON c.oid = con.conrelid
            JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
            JOIN unnest(con.conkey) WITH ORDINALITY AS key_columns(attnum, ordinality) ON true
            JOIN pg_catalog.pg_attribute a ON a.attrelid = c.oid AND a.attnum = key_columns.attnum
            WHERE con.contype = 'u'
              AND """
            + PostgresIntrospectionSupport.nonSystemSchemaPredicate("n.nspname")
            + """
            ORDER BY n.nspname, c.relname, con.conname, key_columns.ordinality
            """;

    public List<UniqueConstraintMetadata> load(Connection connection) throws SQLException {
        Map<ConstraintKey, List<String>> uniqueConstraints = new LinkedHashMap<>();

        try (PreparedStatement statement = connection.prepareStatement(LOAD_UNIQUE_CONSTRAINTS_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                ConstraintKey constraintKey = new ConstraintKey(
                        PostgresIntrospectionSupport.requireIdentifier(resultSet, "schema_name"),
                        PostgresIntrospectionSupport.requireIdentifier(resultSet, "table_name"),
                        PostgresIntrospectionSupport.requireIdentifier(resultSet, "constraint_name")
                );
                uniqueConstraints.computeIfAbsent(constraintKey, ignored -> new ArrayList<>())
                        .add(PostgresIntrospectionSupport.requireIdentifier(resultSet, "column_name"));
            }
        }

        List<UniqueConstraintMetadata> results = new ArrayList<>();
        for (Map.Entry<ConstraintKey, List<String>> entry : uniqueConstraints.entrySet()) {
            results.add(new UniqueConstraintMetadata(
                    entry.getKey().schemaName(),
                    entry.getKey().tableName(),
                    entry.getKey().constraintName(),
                    List.copyOf(entry.getValue())
            ));
        }

        return results;
    }

    private record ConstraintKey(String schemaName, String tableName, String constraintName) {
    }
}

