package com.example.core.postgres.ast;

public record PaginationAst(
        Integer limit,
        Integer offset
) {
}

