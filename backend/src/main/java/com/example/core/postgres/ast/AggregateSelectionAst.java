package com.example.core.postgres.ast;

import com.example.core.postgres.aggregate.AggregateFunction;

public record AggregateSelectionAst(
        String alias,
        AggregateFunction function,
        String field
) {
}

