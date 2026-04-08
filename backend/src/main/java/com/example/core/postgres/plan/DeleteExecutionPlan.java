package com.example.core.postgres.plan;

public record DeleteExecutionPlan(
        Integer datasourceId,
        String tableIdentifier,
        Object rowId
) implements ExecutionPlan {
}

