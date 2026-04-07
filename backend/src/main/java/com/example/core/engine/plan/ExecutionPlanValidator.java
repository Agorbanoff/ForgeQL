package com.example.core.engine.plan;

import com.example.common.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ExecutionPlanValidator {

    public void validate(ExecutionPlan executionPlan) {
        if (executionPlan == null) {
            throw new CustomException("Execution plan is required", HttpStatus.BAD_REQUEST);
        }
        if (executionPlan.datasourceId() == null) {
            throw new CustomException("Execution plan datasource id is required", HttpStatus.BAD_REQUEST);
        }
    }
}
