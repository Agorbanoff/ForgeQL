package com.example.core.postgres.plan;

import com.example.common.exceptions.InvalidExecutionPlanException;
import com.example.common.exceptions.UnsupportedMutationTargetException;
import com.example.core.postgres.ast.InsertMutationAst;
import com.example.core.postgres.execution.ValueCoercionService;
import com.example.core.postgres.schema.SchemaReadService;
import com.example.core.postgres.schema.model.GeneratedSchema;
import com.example.core.postgres.schema.model.SchemaColumn;
import com.example.core.postgres.schema.model.SchemaPrimaryKey;
import com.example.core.postgres.schema.model.SchemaTable;
import com.example.core.postgres.schema.model.SchemaTableType;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class InsertPlanner {

    private final SchemaReadService schemaReadService;
    private final ValueCoercionService valueCoercionService;
    private final ExecutionPlanValidator executionPlanValidator;

    public InsertPlanner(
            SchemaReadService schemaReadService,
            ValueCoercionService valueCoercionService,
            ExecutionPlanValidator executionPlanValidator
    ) {
        this.schemaReadService = schemaReadService;
        this.valueCoercionService = valueCoercionService;
        this.executionPlanValidator = executionPlanValidator;
    }

    public InsertExecutionPlan plan(Integer datasourceId, Integer userId, InsertMutationAst ast) {
        if (ast == null) {
            throw new InvalidExecutionPlanException("Insert mutation AST is required");
        }

        GeneratedSchema schema = schemaReadService.getSchema(datasourceId, userId);
        SchemaTable table = validateInsertableTable(schema, ast.tableIdentifier());
        Map<String, Object> coercedValues = validateAndCoerceValues(ast.values(), table);
        validateRequiredColumns(table, coercedValues);

        InsertExecutionPlan executionPlan = new InsertExecutionPlan(
                datasourceId,
                table.qualifiedName(),
                table.primaryKey().columns().get(0),
                coercedValues,
                buildReturningColumns(table)
        );
        executionPlanValidator.validate(executionPlan);
        return executionPlan;
    }

    private SchemaTable validateInsertableTable(GeneratedSchema schema, String qualifiedTableName) {
        if (schema == null) {
            throw new InvalidExecutionPlanException("Generated schema is required for insert planning");
        }
        if (qualifiedTableName == null || qualifiedTableName.isBlank()) {
            throw new InvalidExecutionPlanException("Insert target table identifier is required");
        }

        String normalizedQualifiedTableName = normalizeRequiredText(qualifiedTableName, "insert target table identifier");
        if (!isQualifiedTableIdentifier(normalizedQualifiedTableName)) {
            throw new InvalidExecutionPlanException(
                    "Insert planner requires a fully qualified table identifier such as public.orders"
            );
        }

        SchemaTable table = schema.tables().get(normalizedQualifiedTableName);
        if (table == null) {
            throw new InvalidExecutionPlanException("Insert target table does not exist in generated schema");
        }
        if (table.tableType() != SchemaTableType.TABLE) {
            throw new UnsupportedMutationTargetException(
                    "Insert mutations are supported only for TABLE objects in v1"
            );
        }
        if (!table.capabilities().insert()) {
            throw new UnsupportedMutationTargetException(
                    "Insert mutations require a TABLE with exactly one primary key column in v1"
            );
        }

        SchemaPrimaryKey primaryKey = table.primaryKey();
        if (primaryKey == null || primaryKey.columns().size() != 1) {
            throw new UnsupportedMutationTargetException(
                    "Insert mutations require exactly one primary key column in v1"
            );
        }

        return table;
    }

    private Map<String, Object> validateAndCoerceValues(Map<String, Object> values, SchemaTable table) {
        if (values == null || values.isEmpty()) {
            throw new InvalidExecutionPlanException("Insert mutation values must not be empty");
        }

        Map<String, SchemaColumn> columnsByName = columnsByName(table);
        for (String fieldName : values.keySet()) {
            SchemaColumn column = columnsByName.get(fieldName);
            if (column == null) {
                throw new InvalidExecutionPlanException(
                        "Insert column does not exist on table " + table.qualifiedName() + ": " + fieldName
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
                    "Insert does not allow writes to generated column " + column.name()
                            + " on table " + table.qualifiedName()
            );
        }
        if (isAutoGeneratedInsertColumn(column)) {
            throw new InvalidExecutionPlanException(
                    "Insert does not allow writes to auto-generated column " + column.name()
                            + " on table " + table.qualifiedName()
            );
        }
        if (!column.capabilities().writable()) {
            throw new InvalidExecutionPlanException(
                    "Insert column is not writable on table " + table.qualifiedName() + ": " + column.name()
            );
        }
    }

    private void validateRequiredColumns(SchemaTable table, Map<String, Object> values) {
        for (SchemaColumn column : table.columns()) {
            if (!isRequiredInsertColumn(column)) {
                continue;
            }
            if (!values.containsKey(column.name())) {
                throw new InvalidExecutionPlanException(
                        "Insert requires column " + column.name() + " on table " + table.qualifiedName()
                );
            }
        }
    }

    private boolean isRequiredInsertColumn(SchemaColumn column) {
        boolean hasDefault = column.defaultValue() != null && !column.defaultValue().isBlank();
        return !column.nullable()
                && !hasDefault
                && !column.identity()
                && !column.generated()
                && column.capabilities().writable();
    }

    private boolean isAutoGeneratedInsertColumn(SchemaColumn column) {
        return column.identity() || hasSequenceGeneratedDefault(column);
    }

    private boolean hasSequenceGeneratedDefault(SchemaColumn column) {
        String defaultValue = column.defaultValue();
        if (defaultValue == null || defaultValue.isBlank()) {
            return false;
        }

        String normalizedDefaultValue = defaultValue.trim().toLowerCase(Locale.ROOT);
        return normalizedDefaultValue.startsWith("nextval(");
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
