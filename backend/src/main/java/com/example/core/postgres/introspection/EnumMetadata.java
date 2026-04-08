package com.example.core.postgres.introspection;

import java.util.List;

public record EnumMetadata(
        String schemaName,
        String enumName,
        List<String> labels
) {
}

