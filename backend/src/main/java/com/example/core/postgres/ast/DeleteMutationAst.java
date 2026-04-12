package com.example.core.postgres.ast;

public record DeleteMutationAst(
        String tableIdentifier,
        Object primaryKeyValue
) {
    public DeleteMutationAst {
        tableIdentifier = tableIdentifier == null ? null : tableIdentifier.trim();
    }
}