package com.example.core.postgres.introspection;

import java.sql.ResultSet;
import java.sql.SQLException;

final class PostgresIntrospectionSupport {

    private PostgresIntrospectionSupport() {
    }

    static String nonSystemSchemaPredicate(String schemaExpression) {
        return "("
                + schemaExpression + " <> 'pg_catalog' "
                + "AND " + schemaExpression + " <> 'information_schema' "
                + "AND " + schemaExpression + " <> 'pg_toast' "
                + "AND " + schemaExpression + " NOT LIKE 'pg_temp_%' "
                + "AND " + schemaExpression + " NOT LIKE 'pg_toast_temp_%'"
                + ")";
    }

    static String requireIdentifier(ResultSet resultSet, String columnLabel) throws SQLException {
        String value = resultSet.getString(columnLabel);
        if (value == null || value.isBlank()) {
            throw new SQLException("PostgreSQL metadata query returned null or blank " + columnLabel);
        }

        return value;
    }

    static String requireValue(ResultSet resultSet, String columnLabel) throws SQLException {
        String value = resultSet.getString(columnLabel);
        if (value == null) {
            throw new SQLException("PostgreSQL metadata query returned null " + columnLabel);
        }

        return value;
    }

    static Integer normalizeNonNegative(Integer value) {
        if (value == null || value < 0) {
            return null;
        }

        return value;
    }
}
