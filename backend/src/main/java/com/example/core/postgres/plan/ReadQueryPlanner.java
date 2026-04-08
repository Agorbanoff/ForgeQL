package com.example.core.postgres.plan;

import com.example.core.postgres.ast.ReadTableAst;
import org.springframework.stereotype.Component;

@Component
public class ReadQueryPlanner {

    public ReadExecutionPlan plan(Integer datasourceId, ReadTableAst ast) {
        throw new UnsupportedOperationException("Read query planning is not implemented yet for datasource " + datasourceId);
    }
}

