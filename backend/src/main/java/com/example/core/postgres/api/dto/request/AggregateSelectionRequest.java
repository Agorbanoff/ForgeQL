package com.example.core.postgres.api.dto.request;

import com.example.core.postgres.aggregate.AggregateFunction;
import jakarta.validation.constraints.NotNull;

public record AggregateSelectionRequest(
        @NotNull(message = "aggregate function is required")
        AggregateFunction function,
        String field,
        String alias
) {
}
