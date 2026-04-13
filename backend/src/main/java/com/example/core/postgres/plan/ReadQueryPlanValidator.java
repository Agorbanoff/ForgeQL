package com.example.core.postgres.plan;

import com.example.common.exceptions.InvalidExecutionPlanException;
import com.example.common.exceptions.InvalidMutationValueException;
import com.example.common.exceptions.UnsupportedMutationTypeException;
import com.example.core.postgres.ast.FilterAst;
import com.example.core.postgres.ast.PaginationAst;
import com.example.core.postgres.ast.ProjectionAst;
import com.example.core.postgres.ast.ReadFilterOperator;
import com.example.core.postgres.ast.SortAst;
import com.example.core.postgres.execution.ValueCoercionService;
import com.example.core.postgres.schema.model.GeneratedSchema;
import com.example.core.postgres.schema.model.SchemaColumn;
import com.example.core.postgres.schema.model.SchemaTable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ReadQueryPlanValidator {

    private final ValueCoercionService valueCoercionService;

    public ReadQueryPlanValidator(ValueCoercionService valueCoercionService) {
        this.valueCoercionService = valueCoercionService;
    }

    public SchemaTable validateReadableTable(GeneratedSchema schema, String qualifiedTableName) {
        if (schema == null) {
            throw new InvalidExecutionPlanException("Generated schema is required for read planning");
        }
        if (qualifiedTableName == null || qualifiedTableName.isBlank()) {
            throw new InvalidExecutionPlanException("Read query table identifier is required");
        }

        String normalizedQualifiedTableName = normalizeRequiredText(qualifiedTableName, "read query table identifier");
        if (!isQualifiedTableIdentifier(normalizedQualifiedTableName)) {
            throw new InvalidExecutionPlanException(
                    "Read query planner requires a fully qualified table identifier such as public.orders"
            );
        }

        SchemaTable table = schema.tables().get(normalizedQualifiedTableName);
        if (table == null) {
            throw new InvalidExecutionPlanException("Read target table does not exist in generated schema");
        }
        if (!table.capabilities().read()) {
            throw new InvalidExecutionPlanException("Read target table is not readable");
        }

        return table;
    }

    public ProjectionAst validateProjection(ProjectionAst projection, SchemaTable table) {
        Map<String, SchemaColumn> columnsByName = columnsByName(table);
        if (projection == null || projection.selectAll()) {
            return new ProjectionAst(table.columns().stream().map(SchemaColumn::name).toList());
        }

        Set<String> seenColumns = new LinkedHashSet<>();
        List<String> validatedColumns = new ArrayList<>();
        for (String columnName : projection.columns()) {
            String normalizedColumnName = normalizeRequiredText(columnName, "projection column");
            if (!columnsByName.containsKey(normalizedColumnName)) {
                throw new InvalidExecutionPlanException(
                        "Projection column does not exist on table " + table.qualifiedName() + ": " + normalizedColumnName
                );
            }
            if (!seenColumns.add(normalizedColumnName)) {
                throw new InvalidExecutionPlanException(
                        "Projection column is duplicated on table " + table.qualifiedName() + ": " + normalizedColumnName
                );
            }
            validatedColumns.add(normalizedColumnName);
        }

        return new ProjectionAst(List.copyOf(validatedColumns));
    }

    public List<FilterAst> validateFilters(List<FilterAst> filters, SchemaTable table) {
        if (filters == null || filters.isEmpty()) {
            return List.of();
        }

        Map<String, SchemaColumn> columnsByName = columnsByName(table);
        List<FilterAst> validatedFilters = new ArrayList<>();
        Map<String, FilterFieldState> filterStatesByField = new LinkedHashMap<>();
        for (FilterAst filter : filters) {
            if (filter == null) {
                throw new InvalidExecutionPlanException("Read query filter definition is required");
            }

            String field = normalizeRequiredText(filter.field(), "filter field");
            SchemaColumn column = columnsByName.get(field);
            if (column == null) {
                throw new InvalidExecutionPlanException(
                        "Filter column does not exist on table " + table.qualifiedName() + ": " + field
                );
            }
            if (!column.capabilities().filterable()) {
                throw new InvalidExecutionPlanException(
                        "Filter column is not filterable on table " + table.qualifiedName() + ": " + field
                );
            }
            if (filter.operator() == null) {
                throw new InvalidExecutionPlanException("Filter operator is required for field " + field);
            }

            validateFieldOperatorCombination(
                    field,
                    filter.operator(),
                    filterStatesByField.computeIfAbsent(field, ignored -> new FilterFieldState())
            );
            validateOperatorCompatibility(field, filter.operator(), column);
            List<Object> validatedValues = validateFilterValues(field, filter.operator(), filter.values());
            List<Object> coercedValues = coerceFilterValues(field, filter.operator(), validatedValues, column);
            validatedFilters.add(new FilterAst(field, filter.operator(), coercedValues));
        }

        return List.copyOf(validatedFilters);
    }

    public List<SortAst> validateSorts(List<SortAst> sorts, SchemaTable table) {
        if (sorts == null || sorts.isEmpty()) {
            return List.of();
        }

        Map<String, SchemaColumn> columnsByName = columnsByName(table);
        List<SortAst> validatedSorts = new ArrayList<>();
        Set<String> seenSortFields = new LinkedHashSet<>();
        for (SortAst sort : sorts) {
            if (sort == null) {
                throw new InvalidExecutionPlanException("Read query sort definition is required");
            }

            String field = normalizeRequiredText(sort.field(), "sort field");
            SchemaColumn column = columnsByName.get(field);
            if (column == null) {
                throw new InvalidExecutionPlanException(
                        "Sort column does not exist on table " + table.qualifiedName() + ": " + field
                );
            }
            if (!column.capabilities().sortable()) {
                throw new InvalidExecutionPlanException(
                        "Sort column is not sortable on table " + table.qualifiedName() + ": " + field
                );
            }
            if (!seenSortFields.add(field)) {
                throw new InvalidExecutionPlanException(
                        "Sort column is duplicated on table " + table.qualifiedName() + ": " + field
                );
            }

            validatedSorts.add(new SortAst(field, sort.direction() == null ? SortAst.Direction.ASC : sort.direction()));
        }

        return List.copyOf(validatedSorts);
    }

    public PaginationAst validatePagination(PaginationAst pagination) {
        if (pagination == null) {
            return new PaginationAst(null, 0);
        }

        Integer limit = pagination.limit();
        Integer offset = pagination.offset();

        if (limit != null && limit <= 0) {
            throw new InvalidExecutionPlanException("Read query limit must be positive");
        }
        if (offset != null && offset < 0) {
            throw new InvalidExecutionPlanException("Read query offset must be zero or positive");
        }

        return new PaginationAst(limit, offset == null ? 0 : offset);
    }

    private void validateFieldOperatorCombination(
            String field,
            ReadFilterOperator operator,
            FilterFieldState filterFieldState
    ) {
        if (operator == ReadFilterOperator.IS_NULL || operator == ReadFilterOperator.IS_NOT_NULL) {
            if (filterFieldState.hasNullOperator() || filterFieldState.hasNonNullOperator()) {
                throw new InvalidExecutionPlanException(
                        "Field " + field + " cannot combine null operators with other operators in v1 read filters"
                );
            }
            filterFieldState.markNullOperator(operator);
            return;
        }

        if (filterFieldState.hasNullOperator()) {
            throw new InvalidExecutionPlanException(
                    "Field " + field + " cannot combine null operators with other operators in v1 read filters"
            );
        }

        filterFieldState.markNonNullOperator();
    }

    private void validateOperatorCompatibility(String field, ReadFilterOperator operator, SchemaColumn column) {
        switch (operator) {
            case LIKE, ILIKE -> {
                if (!isStringLikeColumn(column)) {
                    throw new InvalidExecutionPlanException(
                            filterMessage(field, operator) + " is only allowed on string-like columns"
                    );
                }
            }
            case GT, GTE, LT, LTE, BETWEEN -> {
                if (!isComparableScalarColumn(column)) {
                    throw new InvalidExecutionPlanException(
                            filterMessage(field, operator) + " is only allowed on comparable scalar columns"
                    );
                }
            }
            case IN -> {
                if (!column.capabilities().filterable()) {
                    throw new InvalidExecutionPlanException(
                            filterMessage(field, operator) + " is not allowed on non-filterable columns"
                    );
                }
            }
            case IS_NULL, IS_NOT_NULL -> {
                if (!column.capabilities().filterable()) {
                    throw new InvalidExecutionPlanException(
                            filterMessage(field, operator) + " is only allowed on filterable columns"
                    );
                }
            }
            case EQ, NE -> {
                // Any filterable column is valid for equality operators in v1.
            }
        }
    }

    private List<Object> validateFilterValues(String field, ReadFilterOperator operator, List<Object> values) {
        List<Object> safeValues = values == null ? List.of() : new ArrayList<>(values);

        return switch (operator) {
            case EQ, NE, GT, GTE, LT, LTE -> validateSingleValueOperator(field, operator, safeValues, false);
            case LIKE, ILIKE -> validateSingleValueOperator(field, operator, safeValues, true);
            case IN -> validateInOperator(field, safeValues);
            case BETWEEN -> validateBetweenOperator(field, safeValues);
            case IS_NULL, IS_NOT_NULL -> validateValuelessOperator(field, operator, safeValues);
        };
    }

    private List<Object> validateSingleValueOperator(
            String field,
            ReadFilterOperator operator,
            List<Object> values,
            boolean requireString
    ) {
        if (values.size() != 1) {
            throw new InvalidExecutionPlanException(
                    filterMessage(field, operator) + " requires exactly one value"
            );
        }

        Object value = values.get(0);
        if (value == null) {
            throw new InvalidExecutionPlanException(filterMessage(field, operator) + " cannot be null");
        }
        if (requireString && !(value instanceof String)) {
            throw new InvalidExecutionPlanException(filterMessage(field, operator) + " requires a string value");
        }

        return List.of(value);
    }

    private List<Object> validateInOperator(String field, List<Object> values) {
        if (values.isEmpty()) {
            throw new InvalidExecutionPlanException(filterMessage(field, ReadFilterOperator.IN) + " must not be empty");
        }

        List<Object> validatedValues = new ArrayList<>();
        for (Object value : values) {
            if (value == null) {
                throw new InvalidExecutionPlanException(filterMessage(field, ReadFilterOperator.IN) + " cannot contain null values");
            }
            validatedValues.add(value);
        }

        return List.copyOf(validatedValues);
    }

    private List<Object> validateBetweenOperator(String field, List<Object> values) {
        if (values.size() != 2) {
            throw new InvalidExecutionPlanException(
                    filterMessage(field, ReadFilterOperator.BETWEEN) + " requires exactly two values"
            );
        }

        List<Object> validatedValues = new ArrayList<>(2);
        for (int index = 0; index < values.size(); index++) {
            Object value = values.get(index);
            if (value == null) {
                throw new InvalidExecutionPlanException(
                        filterMessage(field, ReadFilterOperator.BETWEEN) + " cannot contain null values"
                                + " at index " + index
                );
            }
            validatedValues.add(value);
        }

        return List.copyOf(validatedValues);
    }

    private List<Object> validateValuelessOperator(String field, ReadFilterOperator operator, List<Object> values) {
        if (!values.isEmpty()) {
            throw new InvalidExecutionPlanException(filterMessage(field, operator) + " does not accept values");
        }
        return List.of();
    }

    private List<Object> coerceFilterValues(
            String field,
            ReadFilterOperator operator,
            List<Object> values,
            SchemaColumn column
    ) {
        if (operator == ReadFilterOperator.IS_NULL || operator == ReadFilterOperator.IS_NOT_NULL) {
            return List.of();
        }

        List<Object> coercedValues = new ArrayList<>(values.size());
        for (Object value : values) {
            try {
                coercedValues.add(valueCoercionService.coerceValue(column, value));
            } catch (InvalidMutationValueException | UnsupportedMutationTypeException exception) {
                throw new InvalidExecutionPlanException(
                        filterMessage(field, operator) + " is invalid: " + exception.getMessage()
                );
            }
        }

        return List.copyOf(coercedValues);
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

    private boolean isStringLikeColumn(SchemaColumn column) {
        if (column.arrayType() || column.jsonType() || column.jsonbType() || column.enumType() || column.uuidType()) {
            return false;
        }

        String postgresTypeName = normalizeOptionalText(column.postgresTypeName());
        return "text".equals(postgresTypeName)
                || "varchar".equals(postgresTypeName)
                || "bpchar".equals(postgresTypeName)
                || "char".equals(postgresTypeName)
                || "citext".equals(postgresTypeName)
                || "name".equals(postgresTypeName);
    }

    private boolean isComparableScalarColumn(SchemaColumn column) {
        if (column.arrayType() || column.jsonType() || column.jsonbType() || column.enumType() || column.uuidType()) {
            return false;
        }

        if (column.numericType() || column.timestampWithoutTimeZone() || column.timestampWithTimeZone()) {
            return true;
        }

        String postgresTypeName = normalizeOptionalText(column.postgresTypeName());
        return "date".equals(postgresTypeName)
                || "time".equals(postgresTypeName)
                || "timetz".equals(postgresTypeName)
                || "interval".equals(postgresTypeName);
    }

    private String normalizeRequiredText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidExecutionPlanException(fieldName + " is required");
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String filterMessage(String field, ReadFilterOperator operator) {
        return "filter." + field + "." + operator.wireName();
    }

    private static final class FilterFieldState {
        private ReadFilterOperator nullOperator;
        private boolean hasNonNullOperator;

        boolean hasNullOperator() {
            return nullOperator != null;
        }

        boolean hasNonNullOperator() {
            return hasNonNullOperator;
        }

        void markNullOperator(ReadFilterOperator operator) {
            if (nullOperator != null && nullOperator != operator) {
                throw new InvalidExecutionPlanException(
                        "Field cannot enable both isNull and isNotNull in the same read query"
                );
            }
            this.nullOperator = operator;
        }

        void markNonNullOperator() {
            this.hasNonNullOperator = true;
        }
    }
}
