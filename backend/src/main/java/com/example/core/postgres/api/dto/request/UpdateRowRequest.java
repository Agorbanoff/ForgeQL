package com.example.core.postgres.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record UpdateRowRequest(
        @NotEmpty(message = "values must not be empty")
        Map<@NotBlank(message = "values cannot contain blank field names") String, Object> values
) {
    public UpdateRowRequest {
        values = values == null
                ? null
                : Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }
}