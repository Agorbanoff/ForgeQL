package com.example.controller.dtos.response;

import com.example.persistence.Enums.DataSourceConnectionStatus;
import com.example.persistence.Enums.DataSourceStatus;

import java.time.Instant;

public record ResDataSourceConnectionTestDTO(
        Integer datasourceId,
        boolean successful,
        DataSourceStatus status,
        Instant lastConnectionTestAt,
        DataSourceConnectionStatus lastConnectionStatus,
        String lastConnectionError,
        String databaseProductName,
        String serverVersion,
        String message
) {
}
