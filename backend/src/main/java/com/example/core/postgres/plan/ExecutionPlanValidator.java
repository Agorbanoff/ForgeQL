package com.example.core.postgres.plan;

import com.example.common.exceptions.InvalidExecutionPlanException;
import com.example.core.postgres.ast.FilterAst;
import com.example.core.postgres.ast.PaginationAst;
import com.example.core.postgres.ast.ProjectionAst;
import com.example.core.postgres.ast.SortAst;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class ExecutionPlanValidator {

    public void validate(ExecutionPlan executionPlan) {
        if (executionPlan == null) {
            throw new InvalidExecutionPlanException("Execution plan is required");
        }
        if (executionPlan.datasourceId() == null) {
            throw new InvalidExecutionPlanException("Execution plan datasource id is required");
        }
        if (executionPlan.tableIdentifier() == null || executionPlan.tableIdentifier().isBlank()) {
            throw new InvalidExecutionPlanException("Execution plan table identifier is required");
        }
        if (!isQualifiedTableIdentifier(executionPlan.tableIdentifier())) {
            throw new InvalidExecutionPlanException(
                    "Execution plan table identifier must be fully qualified such as public.orders"
            );
        }

        if (executionPlan instanceof ReadExecutionPlan readExecutionPlan) {
            validateReadExecutionPlan(readExecutionPlan);
        }
    }

    private void validateReadExecutionPlan(ReadExecutionPlan executionPlan) {
        ProjectionAst projection = executionPlan.projection();
        List<FilterAst> filters = executionPlan.filters();
        List<SortAst> sorts = executionPlan.sorts();
        PaginationAst pagination = executionPlan.pagination();

        if (projection == null) {
            throw new InvalidExecutionPlanException("Read execution plan projection is required");
        }
        if (filters == null) {
            throw new InvalidExecutionPlanException("Read execution plan filters are required");
        }
        if (sorts == null) {
            throw new InvalidExecutionPlanException("Read execution plan sorts are required");
        }
        if (pagination == null) {
            throw new InvalidExecutionPlanException("Read execution plan pagination is required");
        }

        validateProjection(projection);
        validateSorts(sorts);
        validateFilters(filters);
        validatePagination(pagination);
    }

    private void validateProjection(ProjectionAst projection) {
        Set<String> seenColumns = new LinkedHashSet<>();
        for (String column : projection.columns()) {
            if (column == null || column.isBlank()) {
                throw new InvalidExecutionPlanException("Read execution plan projection columns must be present");
            }
            String normalizedColumn = column.trim();
            if (!seenColumns.add(normalizedColumn)) {
                throw new InvalidExecutionPlanException(
                        "Read execution plan contains duplicate projection column " + normalizedColumn
                );
            }
        }
    }

    private void validateSorts(List<SortAst> sorts) {
        Set<String> seenSortFields = new LinkedHashSet<>();
        for (SortAst sort : sorts) {
            if (sort == null) {
                throw new InvalidExecutionPlanException("Read execution plan sort definition is required");
            }
            if (sort.field() == null || sort.field().isBlank()) {
                throw new InvalidExecutionPlanException("Read execution plan sort field is required");
            }
            String normalizedField = sort.field().trim();
            if (!seenSortFields.add(normalizedField)) {
                throw new InvalidExecutionPlanException(
                        "Read execution plan contains duplicate sort field " + normalizedField
                );
            }
            if (sort.direction() == null) {
                throw new InvalidExecutionPlanException("Read execution plan sort direction is required");
            }
        }
    }

    private void validateFilters(List<FilterAst> filters) {
        for (FilterAst filter : filters) {
            if (filter == null) {
                throw new InvalidExecutionPlanException("Read execution plan filter definition is required");
            }
            if (filter.field() == null || filter.field().isBlank()) {
                throw new InvalidExecutionPlanException("Read execution plan filter field is required");
            }
            if (filter.operator() == null) {
                throw new InvalidExecutionPlanException("Read execution plan filter operator is required");
            }
            if (filter.values() == null) {
                throw new InvalidExecutionPlanException("Read execution plan filter values are required");
            }
        }
    }

    private void validatePagination(PaginationAst pagination) {
        if (pagination.offset() == null) {
            throw new InvalidExecutionPlanException("Read execution plan offset is required");
        }
        if (pagination.offset() < 0) {
            throw new InvalidExecutionPlanException("Read execution plan offset must be zero or positive");
        }
        if (pagination.limit() != null && pagination.limit() <= 0) {
            throw new InvalidExecutionPlanException("Read execution plan limit must be positive");
        }
    }

    private boolean isQualifiedTableIdentifier(String tableIdentifier) {
        String[] parts = tableIdentifier.split("\\.", -1);
        return parts.length == 2 && !parts[0].isBlank() && !parts[1].isBlank();
    }
}

