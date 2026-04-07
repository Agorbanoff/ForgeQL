package com.example.core.engine.schema.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record PostgresSchemaSnapshot(
        Integer datasourceId,
        String serverVersion,
        Instant generatedAt,
        String defaultSchema,
        String fingerprint,
        Map<String, PostgresTableSchema> tables,
        Map<String, List<String>> relationGraph
) {
}
