package com.example.core.postgres.ast;

import com.example.core.postgres.aggregate.AggregateFunction;

public record AggregateSelectionAst(
        String alias,
        AggregateFunction function,
        String field
) {
    public AggregateSelectionAst {
        alias = alias == null ? null : alias.trim();
        field = field == null || field.isBlank() ? null : field.trim();
    }
}