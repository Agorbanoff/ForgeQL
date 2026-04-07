package com.example.core.engine.api.dto.response;

import java.time.Instant;

public record SchemaSummaryResponse(
        Integer datasourceId,
        String fingerprint,
        Instant generatedAt,
        String serverVersion,
        int tableCount
) {
}
