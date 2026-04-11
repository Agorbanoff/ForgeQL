package com.example.core.postgres.api.dto.request;

import jakarta.validation.constraints.Size;

import java.util.List;

public record ReadFieldFilterRequest(
        Object eq,
        Object ne,
        Object gt,
        Object gte,
        Object lt,
        Object lte,
        @Size(min = 1, message = "filter.in must contain at least one value")
        List<Object> in,
        @Size(min = 2, max = 2, message = "filter.between must contain exactly two values")
        List<Object> between,
        String like,
        String ilike,
        Boolean isNull,
        Boolean isNotNull
) {
}
