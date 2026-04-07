package com.example.core.engine.ast;

import java.util.Map;

public record UpdateMutationAst(
        String tableIdentifier,
        Object rowId,
        Map<String, Object> values
) {
}
