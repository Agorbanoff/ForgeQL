package com.example.core.postgres.sql;

import com.example.core.postgres.plan.UpdateExecutionPlan;
import org.springframework.stereotype.Component;

@Component
public class UpdateSqlBuilder {

    public SqlCommand build(UpdateExecutionPlan executionPlan) {
        throw new UnsupportedOperationException("Update SQL building is not implemented yet for table " + executionPlan.tableIdentifier());
    }
}

