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
public class EnumMetadataLoader {

    private static final String LOAD_ENUMS_SQL = """
            SELECT
                n.nspname AS schema_name,
                t.typname AS enum_name,
                e.enumlabel AS enum_label
            FROM pg_catalog.pg_type t
            JOIN pg_catalog.pg_namespace n ON n.oid = t.typnamespace
            JOIN pg_catalog.pg_enum e ON e.enumtypid = t.oid
            WHERE t.typtype = 'e'
              AND """
            + PostgresIntrospectionSupport.nonSystemSchemaPredicate("n.nspname")
            + """
            ORDER BY n.nspname, t.typname, e.enumsortorder
            """;

    public List<EnumMetadata> load(Connection connection) throws SQLException {
        Map<EnumKey, List<String>> enums = new LinkedHashMap<>();

        try (PreparedStatement statement = connection.prepareStatement(LOAD_ENUMS_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                EnumKey enumKey = new EnumKey(
                        PostgresIntrospectionSupport.requireIdentifier(resultSet, "schema_name"),
                        PostgresIntrospectionSupport.requireIdentifier(resultSet, "enum_name")
                );
                enums.computeIfAbsent(enumKey, ignored -> new ArrayList<>())
                        .add(PostgresIntrospectionSupport.requireValue(resultSet, "enum_label"));
            }
        }

        List<EnumMetadata> results = new ArrayList<>();
        for (Map.Entry<EnumKey, List<String>> entry : enums.entrySet()) {
            results.add(new EnumMetadata(
                    entry.getKey().schemaName(),
                    entry.getKey().enumName(),
                    List.copyOf(entry.getValue())
            ));
        }

        return results;
    }

    private record EnumKey(String schemaName, String enumName) {
    }
}

