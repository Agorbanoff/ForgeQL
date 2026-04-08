package com.example.core.postgres.plan;

import com.example.core.postgres.ast.FilterAst;
import com.example.core.postgres.ast.PaginationAst;
import com.example.core.postgres.ast.ProjectionAst;
import com.example.core.postgres.ast.SortAst;

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

