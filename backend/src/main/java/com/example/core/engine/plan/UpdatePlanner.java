package com.example.core.engine.plan;

import com.example.core.engine.ast.UpdateMutationAst;
import org.springframework.stereotype.Component;

@Component
public class UpdatePlanner {

    public UpdateExecutionPlan plan(Integer datasourceId, UpdateMutationAst ast) {
        throw new UnsupportedOperationException("Update planning is not implemented yet for datasource " + datasourceId);
    }
}
