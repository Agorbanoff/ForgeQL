package com.example.core.engine.sql;

import com.example.core.engine.plan.AggregateExecutionPlan;
import org.springframework.stereotype.Component;

@Component
public class AggregateSqlBuilder {

    public SqlCommand build(AggregateExecutionPlan executionPlan) {
        throw new UnsupportedOperationException("Aggregate SQL building is not implemented yet for table " + executionPlan.tableIdentifier());
    }
}
