package com.example.core.postgres.plan;

import com.example.common.exceptions.InvalidExecutionPlanException;
import com.example.core.postgres.ast.AggregateAst;
import com.example.core.postgres.ast.AggregateSelectionAst;
import com.example.core.postgres.ast.FilterAst;
import com.example.core.postgres.schema.SchemaReadService;
import com.example.core.postgres.schema.model.GeneratedSchema;
import com.example.core.postgres.schema.model.SchemaTable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AggregateQueryPlanner {

    private final SchemaReadService schemaReadService;
    private final AggregateQueryPlanValidator aggregateQueryPlanValidator;
    private final ExecutionPlanValidator executionPlanValidator;

    public AggregateQueryPlanner(
            SchemaReadService schemaReadService,
            AggregateQueryPlanValidator aggregateQueryPlanValidator,
            ExecutionPlanValidator executionPlanValidator
    ) {
        this.schemaReadService = schemaReadService;
        this.aggregateQueryPlanValidator = aggregateQueryPlanValidator;
        this.executionPlanValidator = executionPlanValidator;
    }

    public AggregateExecutionPlan plan(Integer datasourceId, Integer userId, AggregateAst ast) {
        if (ast == null) {
            throw new InvalidExecutionPlanException("Aggregate query AST is required");
        }

        GeneratedSchema schema = schemaReadService.getSchema(datasourceId, userId);
        SchemaTable table = aggregateQueryPlanValidator.validateAggregatableTable(schema, ast.tableIdentifier());
        List<AggregateSelectionAst> selections = aggregateQueryPlanValidator.validateSelections(ast.selections(), table);
        List<String> groupBy = aggregateQueryPlanValidator.validateGroupBy(ast.groupBy(), table);
        List<FilterAst> filters = aggregateQueryPlanValidator.validateFilters(ast.filters(), table);

        AggregateExecutionPlan executionPlan = new AggregateExecutionPlan(
                datasourceId,
                table.qualifiedName(),
                selections,
                groupBy,
                filters
        );
        executionPlanValidator.validate(executionPlan);
        return executionPlan;
    }
}

