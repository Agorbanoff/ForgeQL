package com.example.core.engine.mutation;

import java.util.Map;

public record MutationResult(
        long affectedRows,
        Map<String, Object> returnedRow
) {
}
