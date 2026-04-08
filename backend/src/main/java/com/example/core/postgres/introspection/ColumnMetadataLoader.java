package com.example.core.postgres.introspection;

import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Component
public class ColumnMetadataLoader {

    private static final String LOAD_COLUMNS_SQL = """
            SELECT
                n.nspname AS schema_name,
                c.relname AS table_name,
                a.attname AS column_name,
                pg_catalog.format_type(a.atttypid, a.atttypmod) AS db_type,
                type_ns.nspname AS type_schema_name,
                t.typname AS type_name,
                element_ns.nspname AS element_type_schema_name,
                element_t.typname AS element_type_name,
                NOT a.attnotnull AS nullable,
                pg_catalog.pg_get_expr(ad.adbin, ad.adrelid) AS default_value,
                a.attnum AS ordinal_position,
                (a.attidentity <> '') AS identity,
                (a.attgenerated <> '') AS generated,
                CASE
                    WHEN t.typname = 'numeric' AND a.atttypmod >= 4 THEN ((a.atttypmod - 4) >> 16) & 65535
                    WHEN t.typname IN ('time', 'timetz', 'timestamp', 'timestamptz', 'interval') AND a.atttypmod >= 0 THEN a.atttypmod
                    ELSE NULL
                END AS precision,
                CASE
                    WHEN t.typname = 'numeric' AND a.atttypmod >= 4 THEN (a.atttypmod - 4) & 65535
                    ELSE NULL
                END AS scale,
                CASE
                    WHEN t.typname IN ('varchar', 'bpchar', 'varbit', 'bit') AND a.atttypmod >= 4 THEN a.atttypmod - 4
                    ELSE NULL
                END AS length
            FROM pg_catalog.pg_attribute a
            JOIN pg_catalog.pg_class c ON c.oid = a.attrelid
            JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
            JOIN pg_catalog.pg_type t ON t.oid = a.atttypid
            JOIN pg_catalog.pg_namespace type_ns ON type_ns.oid = t.typnamespace
            LEFT JOIN pg_catalog.pg_type element_t ON element_t.oid = t.typelem AND t.typelem <> 0
            LEFT JOIN pg_catalog.pg_namespace element_ns ON element_ns.oid = element_t.typnamespace
            LEFT JOIN pg_catalog.pg_attrdef ad ON ad.adrelid = a.attrelid AND ad.adnum = a.attnum
            WHERE c.relkind IN ('r', 'p', 'v', 'm')
              AND a.attnum > 0
              AND NOT a.attisdropped
              AND """
            + PostgresIntrospectionSupport.nonSystemSchemaPredicate("n.nspname")
            + """
            ORDER BY n.nspname, c.relname, a.attnum
            """;

    public List<ColumnMetadata> load(Connection connection) throws SQLException {
        List<ColumnMetadata> columns = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(LOAD_COLUMNS_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                columns.add(new ColumnMetadata(
                        PostgresIntrospectionSupport.requireIdentifier(resultSet, "schema_name"),
                        PostgresIntrospectionSupport.requireIdentifier(resultSet, "table_name"),
                        PostgresIntrospectionSupport.requireIdentifier(resultSet, "column_name"),
                        PostgresIntrospectionSupport.requireValue(resultSet, "db_type"),
                        resultSet.getString("type_schema_name"),
                        PostgresIntrospectionSupport.requireValue(resultSet, "type_name"),
                        resultSet.getString("element_type_schema_name"),
                        resultSet.getString("element_type_name"),
                        resultSet.getBoolean("nullable"),
                        resultSet.getString("default_value"),
                        resultSet.getInt("ordinal_position"),
                        resultSet.getBoolean("identity"),
                        resultSet.getBoolean("generated"),
                        PostgresIntrospectionSupport.normalizeNonNegative(
                                resultSet.getObject("precision", Integer.class)
                        ),
                        PostgresIntrospectionSupport.normalizeNonNegative(
                                resultSet.getObject("scale", Integer.class)
                        ),
                        PostgresIntrospectionSupport.normalizeNonNegative(
                                resultSet.getObject("length", Integer.class)
                        )
                ));
            }
        }

        return columns;
    }
}

