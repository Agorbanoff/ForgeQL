package com.example.core.postgres.sql;

import com.example.core.postgres.plan.AggregateExecutionPlan;
import org.springframework.stereotype.Component;

@Component
public class AggregateSqlBuilder {

    public SqlCommand build(AggregateExecutionPlan executionPlan) {
        throw new UnsupportedOperationException("Aggregate SQL building is not implemented yet for table " + executionPlan.tableIdentifier());
    }
}

