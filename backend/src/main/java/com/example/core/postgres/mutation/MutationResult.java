package com.example.core.postgres.mutation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record MutationResult(
        MutationType mutationType,
        long affectedRows,
        Object identity,
        Map<String, Object> returnedRow
) {
    public MutationResult {
        returnedRow = returnedRow == null
                ? null
                : Collections.unmodifiableMap(new LinkedHashMap<>(returnedRow));
    }
}