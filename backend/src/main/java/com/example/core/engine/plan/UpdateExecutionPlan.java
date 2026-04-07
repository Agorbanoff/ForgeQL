package com.example.core.engine.plan;

import java.util.Map;

public record UpdateExecutionPlan(
        Integer datasourceId,
        String tableIdentifier,
        Object rowId,
        Map<String, Object> values
) implements ExecutionPlan {
}
