package com.example.core.engine.ast;

public record FilterAst(
        String field,
        String operator,
        Object value
) {
}
