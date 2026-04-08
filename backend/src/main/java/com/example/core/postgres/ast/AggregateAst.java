package com.example.core.postgres.ast;

import java.util.List;

public record AggregateAst(
        String tableIdentifier,
        List<AggregateSelectionAst> selections,
        List<String> groupBy,
        List<FilterAst> filters
) {
}

