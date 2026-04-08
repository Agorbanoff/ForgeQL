package com.example.core.postgres.connection;

import java.util.Properties;

public record PostgresJdbcConnectionConfig(
        String jdbcUrl,
        Properties properties
) {
}
