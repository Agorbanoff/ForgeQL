package com.example.core.postgres.plan;

import com.example.core.postgres.ast.AggregateAst;
import org.springframework.stereotype.Component;

@Component
public class AggregateQueryPlanner {

    public AggregateExecutionPlan plan(Integer datasourceId, AggregateAst ast) {
        throw new UnsupportedOperationException("Aggregate query planning is not implemented yet for datasource " + datasourceId);
    }
}

