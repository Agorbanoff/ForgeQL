package com.example.core.postgres.ast;

public record SortAst(
        String field,
        Direction direction
) {
    public enum Direction {
        ASC,
        DESC
    }
}

