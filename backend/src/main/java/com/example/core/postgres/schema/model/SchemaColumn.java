package com.example.core.postgres.schema.model;

import java.util.List;

public record SchemaColumn(
        String name,
        String dbType,
        String javaType,
        String postgresTypeSchema,
        String postgresTypeName,
        String arrayElementTypeSchema,
        String arrayElementTypeName,
        boolean nullable,
        boolean identity,
        boolean generated,
        String defaultValue,
        int position,
        SchemaColumnCapabilities capabilities,
        Integer precision,
        Integer scale,
        Integer length,
        boolean enumType,
        List<String> enumLabels,
        boolean uuidType,
        boolean jsonType,
        boolean jsonbType,
        boolean arrayType,
        boolean timestampWithoutTimeZone,
        boolean timestampWithTimeZone,
        boolean numericType
) {
}
