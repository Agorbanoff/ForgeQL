package com.example.core.postgres.api.dto.response;

import java.time.Instant;

public record SchemaSummaryResponse(
        Integer datasourceId,
        String fingerprint,
        Instant generatedAt,
        String serverVersion,
        int tableCount
) {
}

