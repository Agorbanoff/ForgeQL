package com.example.core.postgres.ast;

import java.util.List;

public record ProjectionAst(
        List<String> columns
) {
    public ProjectionAst {
        columns = columns == null ? List.of() : List.copyOf(columns);
    }

    public boolean selectAll() {
        return columns.isEmpty();
    }
}

