package com.example.core.postgres.plan;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record InsertExecutionPlan(
        Integer datasourceId,
        String tableIdentifier,
        String primaryKeyColumn,
        Map<String, Object> values,
        List<String> returningColumns
) implements ExecutionPlan {
    public InsertExecutionPlan {
        tableIdentifier = tableIdentifier == null ? null : tableIdentifier.trim();
        primaryKeyColumn = primaryKeyColumn == null ? null : primaryKeyColumn.trim();
        values = values == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(values));
        returningColumns = returningColumns == null ? List.of() : List.copyOf(returningColumns);
    }
}

