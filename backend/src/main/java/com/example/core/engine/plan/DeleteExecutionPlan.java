package com.example.core.engine.plan;

public record DeleteExecutionPlan(
        Integer datasourceId,
        String tableIdentifier,
        Object rowId
) implements ExecutionPlan {
}
