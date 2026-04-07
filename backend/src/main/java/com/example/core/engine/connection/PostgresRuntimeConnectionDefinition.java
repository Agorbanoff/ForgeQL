package com.example.core.engine.connection;

import com.example.persistence.Enums.SslMode;

public record PostgresRuntimeConnectionDefinition(
        Integer datasourceId,
        Integer ownerUserId,
        String displayName,
        String host,
        Integer port,
        String databaseName,
        String schemaName,
        String username,
        String password,
        SslMode sslMode,
        Integer connectTimeoutMs,
        Integer socketTimeoutMs,
        String applicationName,
        String sslRootCertRef,
        String extraJdbcOptionsJson
) {
}
