package com.example.core.postgres.plan;

import com.example.core.postgres.ast.AggregateSelectionAst;
import com.example.core.postgres.ast.FilterAst;

import java.util.List;

public record AggregateExecutionPlan(
        Integer datasourceId,
        String tableIdentifier,
        List<AggregateSelectionAst> selections,
        List<String> groupBy,
        List<FilterAst> filters
) implements ExecutionPlan {
}

