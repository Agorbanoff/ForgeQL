package com.example.core.postgres.schema.model;

public record SchemaTableCapabilities(
        boolean read,
        boolean aggregate,
        boolean insert,
        boolean update,
        boolean delete
) {
}
