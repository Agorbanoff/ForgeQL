package com.example.core.postgres.ast;

import java.util.List;

public record ReadTableAst(
        String tableIdentifier,
        ProjectionAst projection,
        List<FilterAst> filters,
        List<SortAst> sorts,
        PaginationAst pagination
) {
}

