package com.example.core.postgres.ast;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record UpdateMutationAst(
        String tableIdentifier,
        Object primaryKeyValue,
        Map<String, Object> values
) {
    public UpdateMutationAst {
        tableIdentifier = tableIdentifier == null ? null : tableIdentifier.trim();
        values = values == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }
}