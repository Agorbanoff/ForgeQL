package com.example.core.postgres.mutation;

import java.util.Map;

public record MutationResult(
        long affectedRows,
        Map<String, Object> returnedRow
) {
}

