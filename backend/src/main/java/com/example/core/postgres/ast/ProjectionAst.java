package com.example.core.postgres.ast;

import java.util.List;

public record ProjectionAst(
        List<String> columns
) {
}

