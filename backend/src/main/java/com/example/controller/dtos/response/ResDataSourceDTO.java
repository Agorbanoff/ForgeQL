package com.example.controller.dtos.response;

import com.example.persistence.Enums.DataSourceAccessRole;
import com.example.persistence.Enums.DataSourceConnectionStatus;
import com.example.persistence.Enums.DataSourceStatus;
import com.example.persistence.Enums.DatabaseTypes;
import com.example.persistence.Enums.SslMode;

import java.time.Instant;

public record ResDataSourceDTO(
        Integer id,
        Integer ownerUserId,
        String displayName,
        DatabaseTypes dbType,
        String host,
        Integer port,
        String databaseName,
        String schemaName,
        String username,
        SslMode sslMode,
        Integer connectTimeoutMs,
        Integer socketTimeoutMs,
        String applicationName,
        String sslRootCertRef,
        String extraJdbcOptionsJson,
        DataSourceAccessRole accessRole,
        DataSourceStatus status,
        Instant lastConnectionTestAt,
        DataSourceConnectionStatus lastConnectionStatus,
        String lastConnectionError,
        Instant lastSchemaGeneratedAt,
        String lastSchemaFingerprint,
        String serverVersion,
        Instant createdAt,
        Instant updatedAt
) {
}
