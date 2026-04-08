package com.example.core.postgres.schema.model;

public record PostgresTableCapabilities(
        boolean read,
        boolean aggregate,
        boolean insert,
        boolean update,
        boolean delete
) {
}

