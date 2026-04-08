package com.example.core.postgres.schema.model;

import java.util.List;

public record SchemaUniqueConstraint(
        String name,
        List<String> columns
) {
}
