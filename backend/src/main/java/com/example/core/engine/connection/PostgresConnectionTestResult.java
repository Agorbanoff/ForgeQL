package com.example.core.engine.connection;

public record PostgresConnectionTestResult(
        Integer datasourceId,
        boolean successful,
        String message
) {
}
