package com.example.SigmaQL.parser;

import com.example.SigmaQL.common.exceptions.InvalidQueryException;
import com.example.SigmaQL.common.exceptions.UnknownFieldException;
import com.example.SigmaQL.dtos.req.IncludeReqDTO;
import com.example.SigmaQL.dtos.req.QueryReqDTO;
import com.example.SigmaQL.registry.RelationSchema;
import com.example.SigmaQL.registry.SchemaRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class QueryValidator {

    private final SchemaRegistry schemaRegistry;

    public QueryValidator(SchemaRegistry schemaRegistry) {
        this.schemaRegistry = schemaRegistry;
    }

    public void validate(QueryReqDTO dto) throws InvalidQueryException, UnknownFieldException {
        if (dto == null) {
            throw new InvalidQueryException("Query body is missing", HttpStatus.BAD_REQUEST);
        }

        if (dto.getEntity() == null || dto.getEntity().isBlank()) {
            throw new InvalidQueryException("Entity is required", HttpStatus.BAD_REQUEST);
        }

        validateQuery(dto.getEntity(), dto.getFields(), dto.getFilter(), dto.getInclude());
        validatePaging(dto.getLimit(), dto.getOffset());
        validateOrderBy(dto.getEntity(), dto.getOrderBy());
    }

    private void validateQuery(
            String entity,
            List<String> fields,
            Map<String, Map<String, Object>> filter,
            Map<String, IncludeReqDTO> include
    ) throws InvalidQueryException, UnknownFieldException {
        schemaRegistry.getEntity(entity);

        validateFields(entity, fields);
        validateFilter(entity, filter);
        validateInclude(entity, include);
    }

    private void validateFields(String entity, List<String> fields) throws InvalidQueryException, UnknownFieldException {
        if (fields == null || fields.isEmpty()) {
            throw new InvalidQueryException("fields is required and cannot be empty", HttpStatus.BAD_REQUEST);
        }

        for (int i = 0; i < fields.size(); i++) {
            String f = fields.get(i);
            if (f == null || f.isBlank()) {
                throw new InvalidQueryException("fields[" + i + "] is empty", HttpStatus.BAD_REQUEST);
            }
            schemaRegistry.assertFieldExists(entity, f);
        }
    }

    private void validateFilter(String entity, Map<String, Map<String, Object>> filter) throws UnknownFieldException, InvalidQueryException {
        if (filter == null) return;

        for (Map.Entry<String, Map<String, Object>> fieldEntry : filter.entrySet()) {
            String field = fieldEntry.getKey();
            schemaRegistry.assertFieldExists(entity, field);

            Map<String, Object> ops = fieldEntry.getValue();
            if (ops == null || ops.isEmpty()) {
                throw new InvalidQueryException("Filter for field '" + field + "' is empty", HttpStatus.BAD_REQUEST);
            }

            for (Map.Entry<String, Object> opEntry : ops.entrySet()) {
                String opKey = opEntry.getKey();
                Object value = opEntry.getValue();

                Operator op = Operator.fromKey(opKey);
                validateOperatorValue(field, op, value);
            }
        }
    }

    private void validateOperatorValue(String field, Operator op, Object value) throws InvalidQueryException {
        if (value == null) {
            throw new InvalidQueryException("Filter value is null for field '" + field + "'", HttpStatus.BAD_REQUEST);
        }

        if (op == Operator.IN) {
            if (!(value instanceof List<?> list) || list.isEmpty()) {
                throw new InvalidQueryException("Operator 'in' requires a non-empty array for field '" + field + "'", HttpStatus.BAD_REQUEST);
            }
            return;
        }

        if (op == Operator.BETWEEN) {
            if (!(value instanceof List<?> list) || list.size() != 2) {
                throw new InvalidQueryException("Operator 'between' requires an array with 2 values for field '" + field + "'", HttpStatus.BAD_REQUEST);
            }
        }
    }

    private void validateInclude(String entity, Map<String, IncludeReqDTO> include) throws InvalidQueryException, UnknownFieldException {
        if (include == null) return;

        for (Map.Entry<String, IncludeReqDTO> inc : include.entrySet()) {
            String relationName = inc.getKey();
            IncludeReqDTO child = inc.getValue();

            if (relationName == null || relationName.isBlank()) {
                throw new InvalidQueryException("include has an empty relation key", HttpStatus.BAD_REQUEST);
            }
            if (child == null) {
                throw new InvalidQueryException("include." + relationName + " is null", HttpStatus.BAD_REQUEST);
            }

            RelationSchema relationSchema = schemaRegistry.getRelation(entity, relationName);
            String childEntity = relationSchema.getTarget();

            validateQuery(childEntity, child.getFields(), child.getFilter(), child.getInclude());
            validatePaging(child.getLimit(), child.getOffset());
            validateOrderBy(childEntity, child.getOrderBy());
        }
    }

    private void validatePaging(Integer limit, Integer offset) throws InvalidQueryException {
        if (limit != null && limit <= 0) {
            throw new InvalidQueryException("limit must be > 0", HttpStatus.BAD_REQUEST);
        }
        if (offset != null && offset < 0) {
            throw new InvalidQueryException("offset must be >= 0", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateOrderBy(String entity, List<?> orderBy) throws InvalidQueryException, UnknownFieldException {
        if (orderBy == null) return;

        for (int i = 0; i < orderBy.size(); i++) {
            Object item = orderBy.get(i);
            if (item == null) {
                throw new InvalidQueryException("orderBy[" + i + "] is null", HttpStatus.BAD_REQUEST);
            }

            String field;
            String direction;

            try {
                var mGetField = item.getClass().getMethod("getField");
                var mGetDir = item.getClass().getMethod("getDirection");
                field = (String) mGetField.invoke(item);
                direction = (String) mGetDir.invoke(item);
            } catch (Exception e) {
                throw new InvalidQueryException("orderBy[" + i + "] has invalid structure", HttpStatus.BAD_REQUEST);
            }

            if (field == null || field.isBlank()) {
                throw new InvalidQueryException("orderBy[" + i + "].field is empty", HttpStatus.BAD_REQUEST);
            }
            schemaRegistry.assertFieldExists(entity, field);

            if (direction == null || direction.isBlank()) {
                throw new InvalidQueryException("orderBy[" + i + "].direction is empty", HttpStatus.BAD_REQUEST);
            }

            String d = direction.toLowerCase();
            if (!d.equals("asc") && !d.equals("desc")) {
                throw new InvalidQueryException("orderBy[" + i + "].direction must be 'asc' or 'desc'", HttpStatus.BAD_REQUEST);
            }
        }
    }
}