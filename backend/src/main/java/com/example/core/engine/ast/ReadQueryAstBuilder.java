package com.example.core.engine.ast;

import org.springframework.stereotype.Component;

@Component
public class ReadQueryAstBuilder {

    public ReadTableAst build(String tableIdentifier) {
        throw new UnsupportedOperationException("Read AST building is not implemented yet for table " + tableIdentifier);
    }
}
