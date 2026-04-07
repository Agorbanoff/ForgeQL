package com.example.core.engine.plan;

import com.example.core.engine.ast.DeleteMutationAst;
import org.springframework.stereotype.Component;

@Component
public class DeletePlanner {

    public DeleteExecutionPlan plan(Integer datasourceId, DeleteMutationAst ast) {
        throw new UnsupportedOperationException("Delete planning is not implemented yet for datasource " + datasourceId);
    }
}
