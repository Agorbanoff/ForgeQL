package com.example.core.postgres.ast;

import com.example.common.exceptions.InvalidReadFilterException;
import com.example.common.exceptions.MissingRequiredFieldException;
import com.example.core.postgres.api.dto.request.ReadFieldFilterRequest;
import com.example.core.postgres.api.dto.request.ReadRowsRequest;
import com.example.core.postgres.api.dto.request.ReadSortDirection;
import com.example.core.postgres.api.dto.request.ReadSortRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ReadQueryAstBuilder {

    public ReadTableAst build(String qualifiedTableName, ReadRowsRequest request) {
        String normalizedQualifiedTableName = normalizeRequiredText(qualifiedTableName, "tableIdentifier");
        ReadRowsRequest safeRequest = request == null
                ? new ReadRowsRequest(null, null, null, null, null)
                : request;

        return new ReadTableAst(
                normalizedQualifiedTableName,
                buildProjection(safeRequest.columns()),
                buildFilters(safeRequest.filter()),
                buildSorts(safeRequest.sort()),
                new PaginationAst(safeRequest.limit(), safeRequest.offset())
        );
    }

    private ProjectionAst buildProjection(List<String> columns) {
        if (columns == null || columns.isEmpty()) {
            return new ProjectionAst(List.of());
        }

        List<String> normalizedColumns = new ArrayList<>();
        for (String column : columns) {
            normalizedColumns.add(normalizeRequiredText(column, "projection column"));
        }

        return new ProjectionAst(List.copyOf(normalizedColumns));
    }

    private List<FilterAst> buildFilters(Map<String, ReadFieldFilterRequest> filter) {
        if (filter == null || filter.isEmpty()) {
            return List.of();
        }

        LinkedHashMap<String, ReadFieldFilterRequest> orderedFilters = new LinkedHashMap<>();
        filter.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .forEach(entry -> orderedFilters.put(
                        normalizeRequiredText(entry.getKey(), "filter field"),
                        requireFilterDefinition(entry.getValue(), entry.getKey())
                ));

        List<FilterAst> filters = new ArrayList<>();
        for (Map.Entry<String, ReadFieldFilterRequest> entry : orderedFilters.entrySet()) {
            String field = entry.getKey();
            ReadFieldFilterRequest fieldFilter = entry.getValue();

            validateFieldFilter(field, fieldFilter);

            addSingleValueFilter(filters, field, ReadFilterOperator.EQ, fieldFilter.eq());
            addSingleValueFilter(filters, field, ReadFilterOperator.NE, fieldFilter.ne());
            addSingleValueFilter(filters, field, ReadFilterOperator.GT, fieldFilter.gt());
            addSingleValueFilter(filters, field, ReadFilterOperator.GTE, fieldFilter.gte());
            addSingleValueFilter(filters, field, ReadFilterOperator.LT, fieldFilter.lt());
            addSingleValueFilter(filters, field, ReadFilterOperator.LTE, fieldFilter.lte());
            addListValueFilter(filters, field, ReadFilterOperator.IN, fieldFilter.in());
            addListValueFilter(filters, field, ReadFilterOperator.BETWEEN, fieldFilter.between());
            addSingleValueFilter(filters, field, ReadFilterOperator.LIKE, fieldFilter.like());
            addSingleValueFilter(filters, field, ReadFilterOperator.ILIKE, fieldFilter.ilike());
            addValuelessFilter(filters, field, ReadFilterOperator.IS_NULL, fieldFilter.isNull());
            addValuelessFilter(filters, field, ReadFilterOperator.IS_NOT_NULL, fieldFilter.isNotNull());
        }

        return List.copyOf(filters);
    }

    private void validateFieldFilter(String field, ReadFieldFilterRequest filter) {
        if (Boolean.TRUE.equals(filter.isNull()) && Boolean.TRUE.equals(filter.isNotNull())) {
            throw new InvalidReadFilterException(
                    "filter." + field + " cannot enable both isNull and isNotNull at the same time"
            );
        }

        validateListOperator(field, ReadFilterOperator.IN, filter.in(), 1);
        validateListOperator(field, ReadFilterOperator.BETWEEN, filter.between(), 2);
    }

    private List<SortAst> buildSorts(List<ReadSortRequest> sortRequests) {
        if (sortRequests == null || sortRequests.isEmpty()) {
            return List.of();
        }

        List<SortAst> sorts = new ArrayList<>();
        for (ReadSortRequest sortRequest : sortRequests) {
            if (sortRequest == null) {
                throw new MissingRequiredFieldException("sort entry is required");
            }

            sorts.add(new SortAst(
                    normalizeRequiredText(sortRequest.field(), "sort.field"),
                    mapDirection(sortRequest.direction())
            ));
        }

        return List.copyOf(sorts);
    }

    private void addSingleValueFilter(
            List<FilterAst> filters,
            String field,
            ReadFilterOperator operator,
            Object value
    ) {
        if (value == null) {
            return;
        }

        filters.add(new FilterAst(field, operator, List.of(value)));
    }

    private void addListValueFilter(
            List<FilterAst> filters,
            String field,
            ReadFilterOperator operator,
            List<Object> values
    ) {
        if (values == null) {
            return;
        }

        filters.add(new FilterAst(field, operator, copyValues(field, operator, values)));
    }

    private void addValuelessFilter(
            List<FilterAst> filters,
            String field,
            ReadFilterOperator operator,
            Boolean enabled
    ) {
        if (!Boolean.TRUE.equals(enabled)) {
            return;
        }

        filters.add(new FilterAst(field, operator, List.of()));
    }

    private ReadFieldFilterRequest requireFilterDefinition(ReadFieldFilterRequest filter, String field) {
        if (filter == null) {
            throw new MissingRequiredFieldException("filter definition is required for field " + field);
        }
        return filter;
    }

    private SortAst.Direction mapDirection(ReadSortDirection direction) {
        if (direction == null || direction == ReadSortDirection.ASC) {
            return SortAst.Direction.ASC;
        }
        return SortAst.Direction.DESC;
    }

    private void validateListOperator(
            String field,
            ReadFilterOperator operator,
            List<Object> values,
            int expectedMinimumSize
    ) {
        if (values == null) {
            return;
        }
        if (values.isEmpty()) {
            throw new InvalidReadFilterException(errorPrefix(field, operator) + " must not be empty");
        }
        if (operator == ReadFilterOperator.BETWEEN && values.size() != 2) {
            throw new InvalidReadFilterException(errorPrefix(field, operator) + " must contain exactly two values");
        }
        if (operator == ReadFilterOperator.IN && values.size() < expectedMinimumSize) {
            throw new InvalidReadFilterException(errorPrefix(field, operator) + " must contain at least one value");
        }
    }

    private List<Object> copyValues(String field, ReadFilterOperator operator, List<Object> values) {
        List<Object> copiedValues = new ArrayList<>(values.size());
        for (int index = 0; index < values.size(); index++) {
            Object value = values.get(index);
            if (value == null) {
                throw new InvalidReadFilterException(
                        errorPrefix(field, operator) + " cannot contain null values"
                                + (operator == ReadFilterOperator.BETWEEN ? " at index " + index : "")
                );
            }
            copiedValues.add(value);
        }
        return List.copyOf(copiedValues);
    }

    private String errorPrefix(String field, ReadFilterOperator operator) {
        return "filter." + field + "." + operator.wireName();
    }

    private String normalizeRequiredText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new MissingRequiredFieldException(fieldName + " is required");
        }

        return value.trim();
    }
}
