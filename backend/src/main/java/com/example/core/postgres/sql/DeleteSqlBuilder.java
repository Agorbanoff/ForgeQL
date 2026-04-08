package com.example.core.postgres.sql;

import com.example.core.postgres.plan.DeleteExecutionPlan;
import org.springframework.stereotype.Component;

@Component
public class DeleteSqlBuilder {

    public SqlCommand build(DeleteExecutionPlan executionPlan) {
        throw new UnsupportedOperationException("Delete SQL building is not implemented yet for table " + executionPlan.tableIdentifier());
    }
}

