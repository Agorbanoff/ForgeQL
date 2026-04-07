package com.example.core.engine.ast;

public record SortAst(
        String field,
        Direction direction
) {
    public enum Direction {
        ASC,
        DESC
    }
}
