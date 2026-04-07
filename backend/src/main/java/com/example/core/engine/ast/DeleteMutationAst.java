package com.example.core.engine.ast;

public record DeleteMutationAst(
        String tableIdentifier,
        Object rowId
) {
}
