package com.example.core.postgres.plan;

import com.example.core.postgres.ast.UpdateMutationAst;
import org.springframework.stereotype.Component;

@Component
public class UpdatePlanner {

    public UpdateExecutionPlan plan(Integer datasourceId, UpdateMutationAst ast) {
        throw new UnsupportedOperationException("Update planning is not implemented yet for datasource " + datasourceId);
    }
}

