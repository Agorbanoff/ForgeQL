package com.example.core.postgres.ast;

import com.example.common.exceptions.InvalidMutationValueException;
import com.example.common.exceptions.MissingRequiredFieldException;
import com.example.core.postgres.api.dto.request.CreateRowRequest;
import com.example.core.postgres.api.dto.request.UpdateRowRequest;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class MutationAstBuilder {

    public InsertMutationAst buildInsert(String tableIdentifier, CreateRowRequest request) {
        CreateRowRequest safeRequest = request == null ? new CreateRowRequest(null) : request;
        return new InsertMutationAst(
                normalizeRequiredTableIdentifier(tableIdentifier),
                normalizeValues(safeRequest.values(), true)
        );
    }

    public UpdateMutationAst buildUpdate(String tableIdentifier, Object primaryKeyValue, UpdateRowRequest request) {
        UpdateRowRequest safeRequest = request == null ? new UpdateRowRequest(null) : request;
        return new UpdateMutationAst(
                normalizeRequiredTableIdentifier(tableIdentifier),
                normalizePrimaryKeyValue(primaryKeyValue),
                normalizeValues(safeRequest.values(), true)
        );
    }

    public DeleteMutationAst buildDelete(String tableIdentifier, Object primaryKeyValue) {
        return new DeleteMutationAst(
                normalizeRequiredTableIdentifier(tableIdentifier),
                normalizePrimaryKeyValue(primaryKeyValue)
        );
    }

    private String normalizeRequiredTableIdentifier(String tableIdentifier) {
        if (tableIdentifier == null || tableIdentifier.isBlank()) {
            throw new MissingRequiredFieldException("tableIdentifier is required");
        }

        return tableIdentifier.trim();
    }

    private Map<String, Object> normalizeValues(Map<String, Object> values, boolean requireNonEmpty) {
        if (values == null) {
            throw new MissingRequiredFieldException("values is required");
        }
        if (requireNonEmpty && values.isEmpty()) {
            throw new MissingRequiredFieldException("values must not be empty");
        }

        LinkedHashMap<String, Object> normalizedValues = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String fieldName = normalizeRequiredFieldName(entry.getKey());
            normalizedValues.put(fieldName, entry.getValue());
        }

        return Collections.unmodifiableMap(normalizedValues);
    }

    private String normalizeRequiredFieldName(String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            throw new MissingRequiredFieldException("mutation field name is required");
        }

        return fieldName.trim();
    }

    private Object normalizePrimaryKeyValue(Object primaryKeyValue) {
        if (primaryKeyValue == null) {
            throw new MissingRequiredFieldException("primary key value is required");
        }
        if (primaryKeyValue instanceof String stringValue) {
            String normalizedValue = stringValue.trim();
            if (normalizedValue.isEmpty()) {
                throw new MissingRequiredFieldException("primary key value is required");
            }
            return normalizedValue;
        }
        if (primaryKeyValue instanceof Map<?, ?> || primaryKeyValue instanceof Iterable<?> || primaryKeyValue.getClass().isArray()) {
            throw new InvalidMutationValueException(
                    "Mutation primary key value must be a single-column primary key scalar in v1"
            );
        }

        return primaryKeyValue;
    }
}

