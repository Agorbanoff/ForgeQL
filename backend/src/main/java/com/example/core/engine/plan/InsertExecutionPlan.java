package com.example.core.engine.plan;

import java.util.Map;

public record InsertExecutionPlan(
        Integer datasourceId,
        String tableIdentifier,
        Map<String, Object> values
) implements ExecutionPlan {
}
