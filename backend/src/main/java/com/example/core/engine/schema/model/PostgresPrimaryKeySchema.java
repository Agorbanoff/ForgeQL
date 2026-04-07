package com.example.core.engine.schema.model;

import java.util.List;

public record PostgresPrimaryKeySchema(
        List<String> columns
) {
}
