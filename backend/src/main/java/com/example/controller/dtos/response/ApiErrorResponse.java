package com.example.controller.dtos.response;

import java.time.Instant;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        Integer datasourceId,
        String targetPath
) {
}
