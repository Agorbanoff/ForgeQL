package com.example.core.postgres.schema.model;

public record SchemaColumnCapabilities(
        boolean writable,
        boolean filterable,
        boolean sortable,
        boolean aggregatable
) {
}
