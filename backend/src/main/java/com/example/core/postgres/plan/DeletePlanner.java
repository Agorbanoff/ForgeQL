package com.example.core.postgres.plan;

import com.example.common.exceptions.InvalidExecutionPlanException;
import com.example.core.postgres.ast.DeleteMutationAst;
import com.example.core.postgres.execution.ValueCoercionService;
import com.example.core.postgres.schema.SchemaReadService;
import com.example.core.postgres.schema.model.GeneratedSchema;
import com.example.core.postgres.schema.model.SchemaColumn;
import com.example.core.postgres.schema.model.SchemaPrimaryKey;
import com.example.core.postgres.schema.model.SchemaTable;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DeletePlanner {

    private final SchemaReadService schemaReadService;
    private final ValueCoercionService valueCoercionService;
    private final ExecutionPlanValidator executionPlanValidator;

    public DeletePlanner(
            SchemaReadService schemaReadService,
            ValueCoercionService valueCoercionService,
            ExecutionPlanValidator executionPlanValidator
    ) {
        this.schemaReadService = schemaReadService;
        this.valueCoercionService = valueCoercionService;
        this.executionPlanValidator = executionPlanValidator;
    }

    public DeleteExecutionPlan plan(Integer datasourceId, Integer userId, DeleteMutationAst ast) {
        if (ast == null) {
            throw new InvalidExecutionPlanException("Delete mutation AST is required");
        }

        GeneratedSchema schema = schemaReadService.getSchema(datasourceId, userId);
        SchemaTable table = validateDeletableTable(schema, ast.tableIdentifier());
        String primaryKeyColumn = table.primaryKey().columns().get(0);
        Object primaryKeyValue = validatePrimaryKeyScope(ast.primaryKeyValue(), table, primaryKeyColumn);

        DeleteExecutionPlan executionPlan = new DeleteExecutionPlan(
                datasourceId,
                table.qualifiedName(),
                primaryKeyColumn,
                primaryKeyValue,
                buildReturningColumns(table)
        );
        executionPlanValidator.validate(executionPlan);
        return executionPlan;
    }

    private SchemaTable validateDeletableTable(GeneratedSchema schema, String qualifiedTableName) {
        if (schema == null) {
            throw new InvalidExecutionPlanException("Generated schema is required for delete planning");
        }
        if (qualifiedTableName == null || qualifiedTableName.isBlank()) {
            throw new InvalidExecutionPlanException("Delete target table identifier is required");
        }

        String normalizedQualifiedTableName = normalizeRequiredText(qualifiedTableName, "delete target table identifier");
        if (!isQualifiedTableIdentifier(normalizedQualifiedTableName)) {
            throw new InvalidExecutionPlanException(
                    "Delete planner requires a fully qualified table identifier such as public.orders"
            );
        }

        SchemaTable table = schema.tables().get(normalizedQualifiedTableName);
        if (table == null) {
            throw new InvalidExecutionPlanException("Delete target table does not exist in generated schema");
        }
        if (!table.capabilities().delete()) {
            throw new InvalidExecutionPlanException("Delete target table is not deleteable in v1");
        }

        SchemaPrimaryKey primaryKey = table.primaryKey();
        if (primaryKey == null || primaryKey.columns().size() != 1) {
            throw new InvalidExecutionPlanException(
                    "Delete mutations require exactly one primary key column in v1"
            );
        }

        return table;
    }

    private Object validatePrimaryKeyScope(Object primaryKeyValue, SchemaTable table, String primaryKeyColumn) {
        if (primaryKeyValue == null) {
            throw new InvalidExecutionPlanException("Delete primary key value is required");
        }
        if (primaryKeyValue instanceof String stringValue && stringValue.isBlank()) {
            throw new InvalidExecutionPlanException("Delete primary key value is required");
        }

        SchemaColumn primaryKeySchemaColumn = columnsByName(table).get(primaryKeyColumn);
        if (primaryKeySchemaColumn == null) {
            throw new InvalidExecutionPlanException(
                    "Delete primary key column does not exist on table " + table.qualifiedName() + ": " + primaryKeyColumn
            );
        }

        return valueCoercionService.coerceValue(primaryKeySchemaColumn, primaryKeyValue);
    }

    private List<String> buildReturningColumns(SchemaTable table) {
        return table.columns().stream()
                .map(SchemaColumn::name)
                .toList();
    }

    private Map<String, SchemaColumn> columnsByName(SchemaTable table) {
        LinkedHashMap<String, SchemaColumn> columnsByName = new LinkedHashMap<>();
        for (SchemaColumn column : table.columns()) {
            columnsByName.put(column.name(), column);
        }
        return columnsByName;
    }

    private boolean isQualifiedTableIdentifier(String tableIdentifier) {
        String[] parts = tableIdentifier.split("\\.", -1);
        return parts.length == 2 && !parts[0].isBlank() && !parts[1].isBlank();
    }

    private String normalizeRequiredText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidExecutionPlanException(fieldName + " is required");
        }
        return value.trim();
    }
}