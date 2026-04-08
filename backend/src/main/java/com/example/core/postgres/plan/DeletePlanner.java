package com.example.core.postgres.plan;

import com.example.core.postgres.ast.DeleteMutationAst;
import org.springframework.stereotype.Component;

@Component
public class DeletePlanner {

    public DeleteExecutionPlan plan(Integer datasourceId, DeleteMutationAst ast) {
        throw new UnsupportedOperationException("Delete planning is not implemented yet for datasource " + datasourceId);
    }
}

