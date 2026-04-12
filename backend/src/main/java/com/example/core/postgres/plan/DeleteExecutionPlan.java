package com.example.core.postgres.plan;

import java.util.List;

public record DeleteExecutionPlan(
        Integer datasourceId,
        String tableIdentifier,
        String primaryKeyColumn,
        Object primaryKeyValue,
        List<String> returningColumns
) implements ExecutionPlan {
    public DeleteExecutionPlan {
        tableIdentifier = tableIdentifier == null ? null : tableIdentifier.trim();
        primaryKeyColumn = primaryKeyColumn == null ? null : primaryKeyColumn.trim();
        returningColumns = returningColumns == null ? List.of() : List.copyOf(returningColumns);
    }
}