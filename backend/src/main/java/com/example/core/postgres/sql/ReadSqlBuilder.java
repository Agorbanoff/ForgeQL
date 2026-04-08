package com.example.core.postgres.sql;

import com.example.core.postgres.plan.ReadExecutionPlan;
import org.springframework.stereotype.Component;

@Component
public class ReadSqlBuilder {

    public SqlCommand build(ReadExecutionPlan executionPlan) {
        throw new UnsupportedOperationException("Read SQL building is not implemented yet for table " + executionPlan.tableIdentifier());
    }
}

