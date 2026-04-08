package com.example.core.postgres.ast;

import com.example.core.postgres.api.dto.request.AggregateRequest;
import org.springframework.stereotype.Component;

@Component
public class AggregateQueryAstBuilder {

    public AggregateAst build(String tableIdentifier, AggregateRequest request) {
        throw new UnsupportedOperationException("Aggregate AST building is not implemented yet for table " + tableIdentifier);
    }
}

