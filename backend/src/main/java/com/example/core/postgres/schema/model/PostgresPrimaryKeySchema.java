package com.example.core.postgres.schema.model;

import java.util.List;

public record PostgresPrimaryKeySchema(
        List<String> columns
) {
}

