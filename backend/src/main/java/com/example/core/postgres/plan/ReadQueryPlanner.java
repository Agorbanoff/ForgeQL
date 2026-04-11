package com.example.core.postgres.plan;

import com.example.common.exceptions.InvalidExecutionPlanException;
import com.example.core.postgres.ast.ReadTableAst;
import com.example.core.postgres.ast.FilterAst;
import com.example.core.postgres.ast.PaginationAst;
import com.example.core.postgres.ast.ProjectionAst;
import com.example.core.postgres.ast.SortAst;
import com.example.core.postgres.schema.SchemaReadService;
import com.example.core.postgres.schema.model.GeneratedSchema;
import com.example.core.postgres.schema.model.SchemaTable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReadQueryPlanner {

    private final SchemaReadService schemaReadService;
    private final ReadQueryPlanValidator readQueryPlanValidator;
    private final ExecutionPlanValidator executionPlanValidator;

    public ReadQueryPlanner(
            SchemaReadService schemaReadService,
            ReadQueryPlanValidator readQueryPlanValidator,
            ExecutionPlanValidator executionPlanValidator
    ) {
        this.schemaReadService = schemaReadService;
        this.readQueryPlanValidator = readQueryPlanValidator;
        this.executionPlanValidator = executionPlanValidator;
    }

    public ReadExecutionPlan plan(Integer datasourceId, Integer userId, ReadTableAst ast) {
        if (ast == null) {
            throw new InvalidExecutionPlanException("Read query AST is required");
        }

        GeneratedSchema schema = schemaReadService.getSchema(datasourceId, userId);
        SchemaTable table = readQueryPlanValidator.validateReadableTable(schema, ast.tableIdentifier());
        ProjectionAst projection = readQueryPlanValidator.validateProjection(ast.projection(), table);
        List<FilterAst> filters = readQueryPlanValidator.validateFilters(ast.filters(), table);
        List<SortAst> sorts = readQueryPlanValidator.validateSorts(ast.sorts(), table);
        PaginationAst pagination = readQueryPlanValidator.validatePagination(ast.pagination());

        ReadExecutionPlan executionPlan = new ReadExecutionPlan(
                datasourceId,
                table.qualifiedName(),
                projection,
                filters,
                sorts,
                pagination
        );
        executionPlanValidator.validate(executionPlan);
        return executionPlan;
    }
}

