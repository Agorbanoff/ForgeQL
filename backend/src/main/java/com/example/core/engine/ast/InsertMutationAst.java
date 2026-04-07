package com.example.core.engine.ast;

import java.util.Map;

public record InsertMutationAst(
        String tableIdentifier,
        Map<String, Object> values
) {
}
