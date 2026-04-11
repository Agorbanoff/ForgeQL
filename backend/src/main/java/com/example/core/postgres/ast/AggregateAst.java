package com.example.core.postgres.ast;

import java.util.List;

public record AggregateAst(
        String tableIdentifier,
        List<AggregateSelectionAst> selections,
        List<String> groupBy,
        List<FilterAst> filters
) {
    public AggregateAst {
        selections = selections == null ? List.of() : List.copyOf(selections);
        groupBy = groupBy == null ? List.of() : List.copyOf(groupBy);
        filters = filters == null ? List.of() : List.copyOf(filters);
    }
}