package com.example.core.engine.ast;

import com.example.core.engine.aggregate.AggregateFunction;

public record AggregateSelectionAst(
        String alias,
        AggregateFunction function,
        String field
) {
}
