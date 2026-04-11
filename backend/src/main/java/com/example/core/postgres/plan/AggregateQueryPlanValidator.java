package com.example.core.postgres.plan;

import com.example.common.exceptions.InvalidExecutionPlanException;
import com.example.core.postgres.aggregate.AggregateFunction;
import com.example.core.postgres.ast.AggregateSelectionAst;
import com.example.core.postgres.ast.FilterAst;
import com.example.core.postgres.schema.model.GeneratedSchema;
import com.example.core.postgres.schema.model.SchemaColumn;
import com.example.core.postgres.schema.model.SchemaTable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class AggregateQueryPlanValidator {

    private final ReadQueryPlanValidator readQueryPlanValidator;

    public AggregateQueryPlanValidator(ReadQueryPlanValidator readQueryPlanValidator) {
        this.readQueryPlanValidator = readQueryPlanValidator;
    }

    public SchemaTable validateAggregatableTable(GeneratedSchema schema, String qualifiedTableName) {
        if (schema == null) {
            throw new InvalidExecutionPlanException("Generated schema is required for aggregate planning");
        }
        if (qualifiedTableName == null || qualifiedTableName.isBlank()) {
            throw new InvalidExecutionPlanException("Aggregate query table identifier is required");
        }

        String normalizedQualifiedTableName = normalizeRequiredText(qualifiedTableName, "aggregate query table identifier");
        if (!isQualifiedTableIdentifier(normalizedQualifiedTableName)) {
            throw new InvalidExecutionPlanException(
                    "Aggregate query planner requires a fully qualified table identifier such as public.orders"
            );
        }

        SchemaTable table = schema.tables().get(normalizedQualifiedTableName);
        if (table == null) {
            throw new InvalidExecutionPlanException("Aggregate target table does not exist in generated schema");
        }
        if (!table.capabilities().aggregate()) {
            throw new InvalidExecutionPlanException("Aggregate target table is not aggregatable");
        }

        return table;
    }

    public List<AggregateSelectionAst> validateSelections(List<AggregateSelectionAst> selections, SchemaTable table) {
        if (selections == null || selections.isEmpty()) {
            throw new InvalidExecutionPlanException("Aggregate query selections are required");
        }

        Map<String, SchemaColumn> columnsByName = columnsByName(table);
        List<AggregateSelectionAst> validatedSelections = new ArrayList<>();
        Set<String> aliases = new LinkedHashSet<>();
        for (AggregateSelectionAst selection : selections) {
            if (selection == null) {
                throw new InvalidExecutionPlanException("Aggregate selection is required");
            }
            if (selection.function() == null) {
                throw new InvalidExecutionPlanException("Aggregate selection function is required");
            }

            String alias = normalizeRequiredText(selection.alias(), "aggregate selection alias");
            if (!aliases.add(alias)) {
                throw new InvalidExecutionPlanException("Aggregate selection alias is duplicated: " + alias);
            }

            SchemaColumn column = resolveSelectionColumn(columnsByName, selection, table);
            validateFunctionCompatibility(selection.function(), column, table);
            validatedSelections.add(new AggregateSelectionAst(alias, selection.function(), column == null ? null : column.name()));
        }

        return List.copyOf(validatedSelections);
    }

    public List<String> validateGroupBy(List<String> groupBy, SchemaTable table) {
        if (groupBy == null || groupBy.isEmpty()) {
            return List.of();
        }

        Map<String, SchemaColumn> columnsByName = columnsByName(table);
        List<String> validatedGroupBy = new ArrayList<>();
        Set<String> seenFields = new LinkedHashSet<>();
        for (String field : groupBy) {
            String normalizedField = normalizeRequiredText(field, "aggregate groupBy field");
            if (!columnsByName.containsKey(normalizedField)) {
                throw new InvalidExecutionPlanException(
                        "Aggregate groupBy field does not exist on table " + table.qualifiedName() + ": " + normalizedField
                );
            }
            if (!seenFields.add(normalizedField)) {
                throw new InvalidExecutionPlanException(
                        "Aggregate groupBy field is duplicated on table " + table.qualifiedName() + ": " + normalizedField
                );
            }
            validatedGroupBy.add(normalizedField);
        }

        return List.copyOf(validatedGroupBy);
    }

    public List<FilterAst> validateFilters(List<FilterAst> filters, SchemaTable table) {
        return readQueryPlanValidator.validateFilters(filters, table);
    }

    private SchemaColumn resolveSelectionColumn(
            Map<String, SchemaColumn> columnsByName,
            AggregateSelectionAst selection,
            SchemaTable table
    ) {
        String field = normalizeOptionalText(selection.field());
        if (field == null) {
            if (selection.function() == AggregateFunction.COUNT) {
                return null;
            }
            throw new InvalidExecutionPlanException(
                    "Aggregate function " + selection.function().wireName() + " requires a field on table "
                            + table.qualifiedName()
            );
        }

        SchemaColumn column = columnsByName.get(field);
        if (column == null) {
            throw new InvalidExecutionPlanException(
                    "Aggregate field does not exist on table " + table.qualifiedName() + ": " + field
            );
        }

        return column;
    }

    private void validateFunctionCompatibility(AggregateFunction function, SchemaColumn column, SchemaTable table) {
        switch (function) {
            case COUNT -> {
                if (column != null && !columnExistsForCount(column)) {
                    throw new InvalidExecutionPlanException(
                            "Aggregate function count is not supported for field " + column.name()
                                    + " on table " + table.qualifiedName()
                    );
                }
            }
            case SUM, AVG -> {
                requireAggregatableColumn(column, function, table);
                if (!isNumericAggregateColumn(column)) {
                    throw new InvalidExecutionPlanException(
                            "Aggregate function " + function.wireName() + " requires a numeric field on table "
                                    + table.qualifiedName() + ": " + column.name()
                    );
                }
            }
            case MIN, MAX -> {
                requireAggregatableColumn(column, function, table);
                if (!isMinMaxCompatibleColumn(column)) {
                    throw new InvalidExecutionPlanException(
                            "Aggregate function " + function.wireName() + " is not supported for field "
                                    + column.name() + " on table " + table.qualifiedName()
                    );
                }
            }
        }
    }

    private void requireAggregatableColumn(SchemaColumn column, AggregateFunction function, SchemaTable table) {
        if (column == null) {
            throw new InvalidExecutionPlanException(
                    "Aggregate function " + function.wireName() + " requires a field on table " + table.qualifiedName()
            );
        }
        if (!column.capabilities().aggregatable()) {
            throw new InvalidExecutionPlanException(
                    "Aggregate field is not aggregatable on table " + table.qualifiedName() + ": " + column.name()
            );
        }
    }

    private boolean columnExistsForCount(SchemaColumn column) {
        return column != null;
    }

    private boolean isNumericAggregateColumn(SchemaColumn column) {
        if (column == null || column.arrayType() || column.jsonType() || column.jsonbType()) {
            return false;
        }

        String postgresTypeName = normalizeTypeName(column.postgresTypeName());
        return "int2".equals(postgresTypeName)
                || "int4".equals(postgresTypeName)
                || "int8".equals(postgresTypeName)
                || "float4".equals(postgresTypeName)
                || "float8".equals(postgresTypeName)
                || "numeric".equals(postgresTypeName)
                || "money".equals(postgresTypeName);
    }

    private boolean isMinMaxCompatibleColumn(SchemaColumn column) {
        if (column == null || column.arrayType() || column.jsonType() || column.jsonbType()) {
            return false;
        }
        if (column.enumType() || column.uuidType() || column.numericType()
                || column.timestampWithoutTimeZone() || column.timestampWithTimeZone()) {
            return true;
        }

        String postgresTypeName = normalizeTypeName(column.postgresTypeName());
        return "text".equals(postgresTypeName)
                || "varchar".equals(postgresTypeName)
                || "bpchar".equals(postgresTypeName)
                || "char".equals(postgresTypeName)
                || "citext".equals(postgresTypeName)
                || "name".equals(postgresTypeName)
                || "date".equals(postgresTypeName)
                || "time".equals(postgresTypeName)
                || "timetz".equals(postgresTypeName)
                || "interval".equals(postgresTypeName)
                || "bool".equals(postgresTypeName);
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

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeTypeName(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
