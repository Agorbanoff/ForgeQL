package com.example.core.postgres.connection;

import com.example.persistence.Enums.DataSourceConnectionStatus;

import java.time.Instant;

public record PostgresConnectionTestResult(
        Integer datasourceId,
        boolean successful,
        Instant testedAt,
        DataSourceConnectionStatus connectionStatus,
        String databaseProductName,
        String serverVersion,
        String message
) {
}

