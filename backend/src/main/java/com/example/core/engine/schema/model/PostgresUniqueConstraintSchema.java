package com.example.core.engine.schema.model;

import java.util.List;

public record PostgresUniqueConstraintSchema(
        String name,
        List<String> columns
) {
}
