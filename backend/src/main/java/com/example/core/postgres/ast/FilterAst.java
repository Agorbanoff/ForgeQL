package com.example.core.postgres.ast;

import java.util.List;

public record FilterAst(
        String field,
        ReadFilterOperator operator,
        List<Object> values
) {
}

