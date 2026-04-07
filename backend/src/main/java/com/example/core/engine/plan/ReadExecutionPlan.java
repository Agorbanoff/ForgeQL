package com.example.core.engine.plan;

import com.example.core.engine.ast.FilterAst;
import com.example.core.engine.ast.PaginationAst;
import com.example.core.engine.ast.ProjectionAst;
import com.example.core.engine.ast.SortAst;

import java.util.List;

public record ReadExecutionPlan(
        Integer datasourceId,
        String tableIdentifier,
        ProjectionAst projection,
        List<FilterAst> filters,
        List<SortAst> sorts,
        PaginationAst pagination
) implements ExecutionPlan {
}
