package com.example.core.engine.schema.model;

public record PostgresColumnSchema(
        String name,
        String dbType,
        String javaType,
        boolean nullable,
        boolean identity,
        boolean generated,
        String defaultValue,
        int position,
        boolean writable,
        boolean filterable,
        boolean sortable,
        boolean aggregatable,
        Integer precision,
        Integer scale,
        Integer length
) {
}
