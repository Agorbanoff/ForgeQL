package com.example.core.postgres.plan;

import com.example.common.exceptions.InvalidExecutionPlanException;
import org.springframework.stereotype.Component;

@Component
public class ExecutionPlanValidator {

    public void validate(ExecutionPlan executionPlan) {
        if (executionPlan == null) {
            throw new InvalidExecutionPlanException("Execution plan is required");
        }
        if (executionPlan.datasourceId() == null) {
            throw new InvalidExecutionPlanException("Execution plan datasource id is required");
        }
    }
}

