package com.example.core.engine.plan;

import com.example.core.engine.ast.InsertMutationAst;
import org.springframework.stereotype.Component;

@Component
public class InsertPlanner {

    public InsertExecutionPlan plan(Integer datasourceId, InsertMutationAst ast) {
        throw new UnsupportedOperationException("Insert planning is not implemented yet for datasource " + datasourceId);
    }
}
