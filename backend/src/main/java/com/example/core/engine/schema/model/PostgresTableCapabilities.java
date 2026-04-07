package com.example.core.engine.schema.model;

public record PostgresTableCapabilities(
        boolean read,
        boolean aggregate,
        boolean insert,
        boolean update,
        boolean delete
) {
}
