package com.example.core.engine.plan;

import com.example.core.engine.ast.AggregateSelectionAst;
import com.example.core.engine.ast.FilterAst;

import java.util.List;

public record AggregateExecutionPlan(
        Integer datasourceId,
        String tableIdentifier,
        List<AggregateSelectionAst> selections,
        List<String> groupBy,
        List<FilterAst> filters
) implements ExecutionPlan {
}
