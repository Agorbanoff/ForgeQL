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
public class ForeignKeyMetadataLoader {

    private static final String LOAD_FOREIGN_KEYS_SQL = """
            SELECT
                src_ns.nspname AS source_schema_name,
                src.relname AS source_table_name,
                target_ns.nspname AS target_schema_name,
                target.relname AS target_table_name,
                con.conname AS constraint_name,
                src_attr.attname AS source_column_name,
                target_attr.attname AS target_column_name,
                source_columns.ordinality AS column_position
            FROM pg_catalog.pg_constraint con
            JOIN pg_catalog.pg_class src ON src.oid = con.conrelid
            JOIN pg_catalog.pg_namespace src_ns ON src_ns.oid = src.relnamespace
            JOIN pg_catalog.pg_class target ON target.oid = con.confrelid
            JOIN pg_catalog.pg_namespace target_ns ON target_ns.oid = target.relnamespace
            JOIN unnest(con.conkey) WITH ORDINALITY AS source_columns(attnum, ordinality) ON true
            JOIN unnest(con.confkey) WITH ORDINALITY AS target_columns(attnum, ordinality)
                ON target_columns.ordinality = source_columns.ordinality
            JOIN pg_catalog.pg_attribute src_attr
                ON src_attr.attrelid = src.oid AND src_attr.attnum = source_columns.attnum
            JOIN pg_catalog.pg_attribute target_attr
                ON target_attr.attrelid = target.oid AND target_attr.attnum = target_columns.attnum
            WHERE con.contype = 'f'
              AND """
            + PostgresIntrospectionSupport.nonSystemSchemaPredicate("src_ns.nspname")
            + """
              AND """
            + PostgresIntrospectionSupport.nonSystemSchemaPredicate("target_ns.nspname")
            + """
            ORDER BY
                src_ns.nspname,
                src.relname,
                con.conname,
                source_columns.ordinality
            """;

    public List<ForeignKeyMetadata> load(Connection connection) throws SQLException {
        Map<ForeignKeyKey, ForeignKeyColumns> foreignKeys = new LinkedHashMap<>();

        try (PreparedStatement statement = connection.prepareStatement(LOAD_FOREIGN_KEYS_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                ForeignKeyKey foreignKeyKey = new ForeignKeyKey(
                        PostgresIntrospectionSupport.requireIdentifier(resultSet, "source_schema_name"),
                        PostgresIntrospectionSupport.requireIdentifier(resultSet, "source_table_name"),
                        PostgresIntrospectionSupport.requireIdentifier(resultSet, "target_schema_name"),
                        PostgresIntrospectionSupport.requireIdentifier(resultSet, "target_table_name"),
                        PostgresIntrospectionSupport.requireIdentifier(resultSet, "constraint_name")
                );
                ForeignKeyColumns columns = foreignKeys.computeIfAbsent(
                        foreignKeyKey,
                        ignored -> new ForeignKeyColumns(new ArrayList<>(), new ArrayList<>())
                );
                columns.sourceColumns().add(
                        PostgresIntrospectionSupport.requireIdentifier(resultSet, "source_column_name")
                );
                columns.targetColumns().add(
                        PostgresIntrospectionSupport.requireIdentifier(resultSet, "target_column_name")
                );
            }
        }

        List<ForeignKeyMetadata> results = new ArrayList<>();
        for (Map.Entry<ForeignKeyKey, ForeignKeyColumns> entry : foreignKeys.entrySet()) {
            results.add(new ForeignKeyMetadata(
                    entry.getKey().sourceSchemaName(),
                    entry.getKey().sourceTableName(),
                    entry.getKey().targetSchemaName(),
                    entry.getKey().targetTableName(),
                    entry.getKey().constraintName(),
                    List.copyOf(entry.getValue().sourceColumns()),
                    List.copyOf(entry.getValue().targetColumns())
            ));
        }

        return results;
    }

    private record ForeignKeyKey(
            String sourceSchemaName,
            String sourceTableName,
            String targetSchemaName,
            String targetTableName,
            String constraintName
    ) {
    }

    private record ForeignKeyColumns(List<String> sourceColumns, List<String> targetColumns) {
    }
}

