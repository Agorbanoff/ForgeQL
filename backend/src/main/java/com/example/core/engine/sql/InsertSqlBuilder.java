package com.example.core.engine.sql;

import com.example.core.engine.plan.InsertExecutionPlan;
import org.springframework.stereotype.Component;

@Component
public class InsertSqlBuilder {

    public SqlCommand build(InsertExecutionPlan executionPlan) {
        throw new UnsupportedOperationException("Insert SQL building is not implemented yet for table " + executionPlan.tableIdentifier());
    }
}
