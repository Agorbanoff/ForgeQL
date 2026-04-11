package com.example.core.postgres.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ReadSortRequest(
        @NotBlank(message = "sort.field is required")
        String field,
        ReadSortDirection direction
) {
}
