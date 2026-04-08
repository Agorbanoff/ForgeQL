package com.example.core.postgres.ast;

public record FilterAst(
        String field,
        String operator,
        Object value
) {
}

