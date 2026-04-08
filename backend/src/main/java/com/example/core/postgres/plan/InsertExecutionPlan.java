package com.example.core.postgres.plan;

import java.util.Map;

public record InsertExecutionPlan(
        Integer datasourceId,
        String tableIdentifier,
        Map<String, Object> values
) implements ExecutionPlan {
}

