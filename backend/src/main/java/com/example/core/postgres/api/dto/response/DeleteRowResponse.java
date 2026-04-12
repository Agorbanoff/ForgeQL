package com.example.core.postgres.api.dto.response;

public record DeleteRowResponse(
        long affectedRows,
        Object deletedIdentity
) {
}