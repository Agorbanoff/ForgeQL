package com.example.core.postgres.schema.model;

import java.util.List;

public record PostgresUniqueConstraintSchema(
        String name,
        List<String> columns
) {
}

