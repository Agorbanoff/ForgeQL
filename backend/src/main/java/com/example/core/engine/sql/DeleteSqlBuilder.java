package com.example.core.engine.sql;

import com.example.core.engine.plan.DeleteExecutionPlan;
import org.springframework.stereotype.Component;

@Component
public class DeleteSqlBuilder {

    public SqlCommand build(DeleteExecutionPlan executionPlan) {
        throw new UnsupportedOperationException("Delete SQL building is not implemented yet for table " + executionPlan.tableIdentifier());
    }
}
