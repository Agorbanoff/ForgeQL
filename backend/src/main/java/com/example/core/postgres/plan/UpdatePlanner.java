package com.example.core.postgres.plan;

import com.example.common.exceptions.InvalidExecutionPlanException;
import com.example.core.postgres.ast.UpdateMutationAst;
import com.example.core.postgres.execution.ValueCoercionService;
import com.example.core.postgres.schema.SchemaReadService;
import com.example.core.postgres.schema.model.GeneratedSchema;
import com.example.core.postgres.schema.model.SchemaColumn;
import com.example.core.postgres.schema.model.SchemaPrimaryKey;
import com.example.core.postgres.schema.model.SchemaTable;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class UpdatePlanner {

    private final SchemaReadService schemaReadService;
    private final ValueCoercionService valueCoercionService;
    private final ExecutionPlanValidator executionPlanValidator;

    public UpdatePlanner(
            SchemaReadService schemaReadService,
            ValueCoercionService valueCoercionService,
            ExecutionPlanValidator executionPlanValidator
    ) {
        this.schemaReadService = schemaReadService;
        this.valueCoercionService = valueCoercionService;
        this.executionPlanValidator = executionPlanValidator;
    }

    public UpdateExecutionPlan plan(Integer datasourceId, Integer userId, UpdateMutationAst ast) {
        if (ast == null) {
            throw new InvalidExecutionPlanException("Update mutation AST is required");
        }

        GeneratedSchema schema = schemaReadService.getSchema(datasourceId, userId);
        SchemaTable table = validateUpdatableTable(schema, ast.tableIdentifier());
        Map<String, Object> coercedValues = validateAndCoerceValues(ast.values(), table);
        String primaryKeyColumn = table.primaryKey().columns().get(0);
        Object primaryKeyValue = validatePrimaryKeyScope(ast.primaryKeyValue(), table, primaryKeyColumn);

        UpdateExecutionPlan executionPlan = new UpdateExecutionPlan(
                datasourceId,
                table.qualifiedName(),
                primaryKeyColumn,
                primaryKeyValue,
                coercedValues,
                buildReturningColumns(table)
        );
        executionPlanValidator.validate(executionPlan);
        return executionPlan;
    }

    private SchemaTable validateUpdatableTable(GeneratedSchema schema, String qualifiedTableName) {
        if (schema == null) {
            throw new InvalidExecutionPlanException("Generated schema is required for update planning");
        }
        if (qualifiedTableName == null || qualifiedTableName.isBlank()) {
            throw new InvalidExecutionPlanException("Update target table identifier is required");
        }

        String normalizedQualifiedTableName = normalizeRequiredText(qualifiedTableName, "update target table identifier");
        if (!isQualifiedTableIdentifier(normalizedQualifiedTableName)) {
            throw new InvalidExecutionPlanException(
                    "Update planner requires a fully qualified table identifier such as public.orders"
            );
        }

        SchemaTable table = schema.tables().get(normalizedQualifiedTableName);
        if (table == null) {
            throw new InvalidExecutionPlanException("Update target table does not exist in generated schema");
        }
        if (!table.capabilities().update()) {
            throw new InvalidExecutionPlanException("Update target table is not updateable in v1");
        }

        SchemaPrimaryKey primaryKey = table.primaryKey();
        if (primaryKey == null || primaryKey.columns().size() != 1) {
            throw new InvalidExecutionPlanException(
                    "Update mutations require exactly one primary key column in v1"
            );
        }

        return table;
    }

    private Map<String, Object> validateAndCoerceValues(Map<String, Object> values, SchemaTable table) {
        if (values == null || values.isEmpty()) {
            throw new InvalidExecutionPlanException("Update mutation values must not be empty");
        }

        Map<String, SchemaColumn> columnsByName = columnsByName(table);
        for (String fieldName : values.keySet()) {
            SchemaColumn column = columnsByName.get(fieldName);
            if (column == null) {
                throw new InvalidExecutionPlanException(
                        "Update column does not exist on table " + table.qualifiedName() + ": " + fieldName
                );
            }
            validateWritableColumn(column, table);
        }

        LinkedHashMap<String, Object> coercedValues = new LinkedHashMap<>();
        for (SchemaColumn column : table.columns()) {
            if (!values.containsKey(column.name())) {
                continue;
            }
            coercedValues.put(
                    column.name(),
                    valueCoercionService.coerceValue(column, values.get(column.name()))
            );
        }

        return Collections.unmodifiableMap(new LinkedHashMap<>(coercedValues));
    }

    private void validateWritableColumn(SchemaColumn column, SchemaTable table) {
        if (column.generated()) {
            throw new InvalidExecutionPlanException(
                    "Update does not allow writes to generated column " + column.name()
                            + " on table " + table.qualifiedName()
            );
        }
        if (column.identity()) {
            throw new InvalidExecutionPlanException(
                    "Update does not allow writes to identity column " + column.name()
                            + " on table " + table.qualifiedName()
            );
        }
        if (!column.capabilities().writable()) {
            throw new InvalidExecutionPlanException(
                    "Update column is not writable on table " + table.qualifiedName() + ": " + column.name()
            );
        }
    }

    private Object validatePrimaryKeyScope(Object primaryKeyValue, SchemaTable table, String primaryKeyColumn) {
        if (primaryKeyValue == null) {
            throw new InvalidExecutionPlanException("Update primary key value is required");
        }
        if (primaryKeyValue instanceof String stringValue && stringValue.isBlank()) {
            throw new InvalidExecutionPlanException("Update primary key value is required");
        }

        SchemaColumn primaryKeySchemaColumn = columnsByName(table).get(primaryKeyColumn);
        if (primaryKeySchemaColumn == null) {
            throw new InvalidExecutionPlanException(
                    "Update primary key column does not exist on table " + table.qualifiedName() + ": " + primaryKeyColumn
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
