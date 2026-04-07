package com.example.core.engine.ast;

import com.example.core.engine.api.dto.request.CreateRowRequest;
import com.example.core.engine.api.dto.request.UpdateRowRequest;
import org.springframework.stereotype.Component;

@Component
public class MutationAstBuilder {

    public InsertMutationAst buildInsert(String tableIdentifier, CreateRowRequest request) {
        throw new UnsupportedOperationException("Insert AST building is not implemented yet for table " + tableIdentifier);
    }

    public UpdateMutationAst buildUpdate(String tableIdentifier, Object rowId, UpdateRowRequest request) {
        throw new UnsupportedOperationException("Update AST building is not implemented yet for table " + tableIdentifier);
    }

    public DeleteMutationAst buildDelete(String tableIdentifier, Object rowId) {
        throw new UnsupportedOperationException("Delete AST building is not implemented yet for table " + tableIdentifier);
    }
}
