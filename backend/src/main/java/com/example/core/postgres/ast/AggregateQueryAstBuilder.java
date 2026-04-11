package com.example.core.postgres.ast;

import com.example.common.exceptions.InvalidAggregateRequestException;
import com.example.common.exceptions.MissingRequiredFieldException;
import com.example.core.postgres.api.dto.request.AggregateRequest;
import com.example.core.postgres.api.dto.request.AggregateSelectionRequest;
import com.example.core.postgres.api.dto.request.ReadRowsRequest;
import com.example.core.postgres.aggregate.AggregateFunction;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class AggregateQueryAstBuilder {

    private final ReadQueryAstBuilder readQueryAstBuilder;

    public AggregateQueryAstBuilder(ReadQueryAstBuilder readQueryAstBuilder) {
        this.readQueryAstBuilder = readQueryAstBuilder;
    }

    public AggregateAst build(String tableIdentifier, AggregateRequest request) {
        String normalizedTableIdentifier = normalizeRequiredText(tableIdentifier, "tableIdentifier");
        AggregateRequest safeRequest = request == null
                ? new AggregateRequest(null, null, null)
                : request;

        return new AggregateAst(
                normalizedTableIdentifier,
                buildSelections(safeRequest.selections()),
                buildGroupBy(safeRequest.groupBy()),
                buildFilters(normalizedTableIdentifier, safeRequest)
        );
    }

    private List<AggregateSelectionAst> buildSelections(List<AggregateSelectionRequest> selections) {
        if (selections == null || selections.isEmpty()) {
            throw new MissingRequiredFieldException("selections is required");
        }

        List<AggregateSelectionAst> aggregateSelections = new ArrayList<>();
        Set<String> aliases = new LinkedHashSet<>();
        for (int index = 0; index < selections.size(); index++) {
            AggregateSelectionRequest selection = selections.get(index);
            if (selection == null) {
                throw new MissingRequiredFieldException("aggregate selection is required at index " + index);
            }
            if (selection.function() == null) {
                throw new MissingRequiredFieldException("aggregate selection function is required at index " + index);
            }

            AggregateFunction function = selection.function();
            String field = validateSelectionField(function, selection.field());

            String alias = normalizeOptionalText(selection.alias());
            String resolvedAlias = alias == null ? defaultAlias(function, field) : alias;
            if (!aliases.add(resolvedAlias)) {
                throw new InvalidAggregateRequestException(
                        "aggregate selection alias must be unique: " + resolvedAlias
                );
            }

            aggregateSelections.add(new AggregateSelectionAst(resolvedAlias, function, field));
        }

        return List.copyOf(aggregateSelections);
    }

    private List<String> buildGroupBy(List<String> groupBy) {
        if (groupBy == null || groupBy.isEmpty()) {
            return List.of();
        }

        List<String> fields = new ArrayList<>();
        Set<String> seenFields = new LinkedHashSet<>();
        for (String field : groupBy) {
            String normalizedField = normalizeRequiredText(field, "groupBy field");
            if (!seenFields.add(normalizedField)) {
                throw new InvalidAggregateRequestException("groupBy field is duplicated: " + normalizedField);
            }
            fields.add(normalizedField);
        }

        return List.copyOf(fields);
    }

    private List<FilterAst> buildFilters(String tableIdentifier, AggregateRequest request) {
        ReadRowsRequest readRowsRequest = new ReadRowsRequest(
                null,
                request.filter(),
                null,
                null,
                null
        );
        return readQueryAstBuilder.build(tableIdentifier, readRowsRequest).filters();
    }

    private String validateSelectionField(AggregateFunction function, String rawField) {
        String field = normalizeOptionalText(rawField);

        return switch (function) {
            case COUNT -> field;
            case SUM, AVG, MIN, MAX -> {
                if (field == null) {
                    throw new MissingRequiredFieldException(
                            "aggregate selection field is required for function " + function.wireName()
                    );
                }
                yield field;
            }
        };
    }

    private String defaultAlias(AggregateFunction function, String field) {
        if (field == null) {
            return function.wireName();
        }
        return function.wireName() + "_" + field;
    }

    private String normalizeRequiredText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new MissingRequiredFieldException(fieldName + " is required");
        }

        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}

