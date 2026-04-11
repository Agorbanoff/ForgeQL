package com.example.core.postgres.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Map;

public record AggregateRequest(
        @NotEmpty(message = "selections is required")
        List<@Valid AggregateSelectionRequest> selections,
        List<@NotBlank(message = "groupBy cannot contain blank values") String> groupBy,
        Map<@NotBlank(message = "filter fields cannot be blank") String, @Valid ReadFieldFilterRequest> filter
) {
}

