package com.example.core.postgres.ast;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record InsertMutationAst(
        String tableIdentifier,
        Map<String, Object> values
) {
    public InsertMutationAst {
        tableIdentifier = tableIdentifier == null ? null : tableIdentifier.trim();
        values = values == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }
}