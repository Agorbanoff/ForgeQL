package com.example.core.engine.sql;

import com.example.core.engine.plan.UpdateExecutionPlan;
import org.springframework.stereotype.Component;

@Component
public class UpdateSqlBuilder {

    public SqlCommand build(UpdateExecutionPlan executionPlan) {
        throw new UnsupportedOperationException("Update SQL building is not implemented yet for table " + executionPlan.tableIdentifier());
    }
}
