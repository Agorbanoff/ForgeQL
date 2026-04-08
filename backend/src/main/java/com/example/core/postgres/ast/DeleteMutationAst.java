package com.example.core.postgres.ast;

public record DeleteMutationAst(
        String tableIdentifier,
        Object rowId
) {
}

