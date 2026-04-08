package com.example.core.postgres.ast;

import java.util.Map;

public record InsertMutationAst(
        String tableIdentifier,
        Map<String, Object> values
) {
}

