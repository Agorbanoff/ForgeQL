package com.example.core.postgres.plan;

import com.example.core.postgres.ast.InsertMutationAst;
import org.springframework.stereotype.Component;

@Component
public class InsertPlanner {

    public InsertExecutionPlan plan(Integer datasourceId, InsertMutationAst ast) {
        throw new UnsupportedOperationException("Insert planning is not implemented yet for datasource " + datasourceId);
    }
}

