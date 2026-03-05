package com.example.SigmaQL.sql;

import com.example.SigmaQL.common.exceptions.InvalidQueryException;
import com.example.SigmaQL.common.exceptions.UnknownFieldException;
import com.example.SigmaQL.dtos.req.IncludeReqDTO;
import com.example.SigmaQL.dtos.req.QueryReqDTO;
import com.example.SigmaQL.registry.RelationSchema;
import com.example.SigmaQL.registry.SchemaRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class SqlBuilder {

    private final SchemaRegistry schemaRegistry;

    public SqlBuilder(SchemaRegistry schemaRegistry) {
        this.schemaRegistry = schemaRegistry;
    }

    public SqlPlan build(QueryReqDTO dto) throws InvalidQueryException, UnknownFieldException {
        String entity = dto.getEntity();
        String table = schemaRegistry.getTable(entity);
        if (table == null || table.isBlank()) {
            throw new InvalidQueryException("Table is missing for entity: " + entity, HttpStatus.BAD_REQUEST);
        }

        List<Object> params = new ArrayList<>();

        String select = buildSelect(entity, table, dto.getFields());
        String where = buildWhere(entity, table, dto.getFilter(), params);
        String orderBy = buildOrderBy(entity, table, dto.getOrderBy());
        String paging = buildPaging(dto.getLimit(), dto.getOffset(), params);

        String sql = select + " FROM " + table + where + orderBy + paging;
        return new SqlPlan(sql, params);
    }

    private String buildSelect(String entity, String table, List<String> fields)
            throws InvalidQueryException, UnknownFieldException {

        if (fields == null || fields.isEmpty()) {
            throw new InvalidQueryException("fields is required", HttpStatus.BAD_REQUEST);
        }

        StringJoiner sj = new StringJoiner(", ");
        for (String f : fields) {
            if (f == null || f.isBlank()) continue;
            schemaRegistry.assertFieldExists(entity, f);
            sj.add(table + "." + f);
        }

        String out = sj.toString();
        if (out.isBlank()) {
            throw new InvalidQueryException("fields is required", HttpStatus.BAD_REQUEST);
        }

        return "SELECT " + out;
    }

    private String buildWhere(String entity, String table, Map<String, Map<String, Object>> filter, List<Object> params)
            throws InvalidQueryException, UnknownFieldException {

        if (filter == null || filter.isEmpty()) return "";

        List<String> parts = new ArrayList<>();

        for (var fieldEntry : filter.entrySet()) {
            String field = fieldEntry.getKey();
            if (field == null || field.isBlank()) continue;

            schemaRegistry.assertFieldExists(entity, field);

            Map<String, Object> ops = fieldEntry.getValue();
            if (ops == null || ops.isEmpty()) continue;

            String col = table + "." + field;

            for (var opEntry : ops.entrySet()) {
                String opKey = String.valueOf(opEntry.getKey()).toLowerCase(Locale.ROOT);
                Object value = opEntry.getValue();

                switch (opKey) {
                    case "eq" -> { parts.add(col + " = ?"); params.add(value); }
                    case "ne" -> { parts.add(col + " <> ?"); params.add(value); }
                    case "gt" -> { parts.add(col + " > ?"); params.add(value); }
                    case "gte" -> { parts.add(col + " >= ?"); params.add(value); }
                    case "lt" -> { parts.add(col + " < ?"); params.add(value); }
                    case "lte" -> { parts.add(col + " <= ?"); params.add(value); }
                    case "like" -> { parts.add(col + " LIKE ?"); params.add(value); }
                    case "in" -> {
                        if (value instanceof List<?> list && !list.isEmpty()) {
                            parts.add(buildInClause(col, (List<Object>) (List<?>) list, params));
                        } else {
                            parts.add("1=0");
                        }
                    }
                    case "between" -> {
                        if (value instanceof List<?> list && list.size() == 2) {
                            parts.add(col + " BETWEEN ? AND ?");
                            params.add(list.get(0));
                            params.add(list.get(1));
                        } else {
                            throw new InvalidQueryException(
                                    "between expects list of 2 values for field: " + field,
                                    HttpStatus.BAD_REQUEST
                            );
                        }
                    }
                    default -> throw new InvalidQueryException("Unknown operator: " + opKey, HttpStatus.BAD_REQUEST);
                }
            }
        }

        if (parts.isEmpty()) return "";
        return " WHERE " + String.join(" AND ", parts);
    }

    private String buildInClause(String col, List<Object> values, List<Object> params) {
        StringJoiner sj = new StringJoiner(", ", "(", ")");
        for (Object v : values) {
            sj.add("?");
            params.add(v);
        }
        return col + " IN " + sj;
    }

    private String buildOrderBy(String entity, String table, List<?> orderBy)
            throws InvalidQueryException, UnknownFieldException {

        if (orderBy == null || orderBy.isEmpty()) return "";

        List<String> parts = new ArrayList<>();

        for (Object item : orderBy) {
            if (item == null) continue;

            String field;
            String dir;

            try {
                var mGetField = item.getClass().getMethod("getField");
                var mGetDir = item.getClass().getMethod("getDirection");
                field = (String) mGetField.invoke(item);
                dir = (String) mGetDir.invoke(item);
            } catch (Exception e) {
                throw new InvalidQueryException("Invalid orderBy item", HttpStatus.BAD_REQUEST);
            }

            if (field == null || field.isBlank()) continue;
            schemaRegistry.assertFieldExists(entity, field);

            String d = (dir == null) ? "asc" : dir.toLowerCase(Locale.ROOT);
            if (!d.equals("asc") && !d.equals("desc")) d = "asc";

            parts.add(table + "." + field + " " + d);
        }

        if (parts.isEmpty()) return "";
        return " ORDER BY " + String.join(", ", parts);
    }

    private String buildPaging(Integer limit, Integer offset, List<Object> params) {
        StringBuilder sb = new StringBuilder();
        if (limit != null) { sb.append(" LIMIT ?"); params.add(limit); }
        if (offset != null) { sb.append(" OFFSET ?"); params.add(offset); }
        return sb.toString();
    }

    public SqlPlan buildIncludePlan(
            String parentEntity,
            RelationSchema rel,
            Collection<Object> parentKeys,
            IncludeReqDTO include
    ) throws InvalidQueryException, UnknownFieldException {

        if (parentKeys == null || parentKeys.isEmpty()) {
            return new SqlPlan("SELECT 1 WHERE 1=0", List.of());
        }

        String childEntity = rel.getTarget();
        String childTable = schemaRegistry.getTable(childEntity);
        if (childTable == null || childTable.isBlank()) {
            throw new InvalidQueryException("Table is missing for entity: " + childEntity, HttpStatus.BAD_REQUEST);
        }

        List<Object> params = new ArrayList<>();

        if (include.getFields() == null || include.getFields().isEmpty()) {
            throw new InvalidQueryException("include.fields is required for " + childEntity, HttpStatus.BAD_REQUEST);
        }

        StringJoiner sj = new StringJoiner(", ");
        for (String f : include.getFields()) {
            if (f == null || f.isBlank()) continue;
            schemaRegistry.assertFieldExists(childEntity, f);
            sj.add(childTable + "." + f);
        }

        String selectCols = sj.toString();
        if (selectCols.isBlank()) {
            throw new InvalidQueryException("include.fields is required for " + childEntity, HttpStatus.BAD_REQUEST);
        }

        String select = "SELECT " + selectCols;

        String matchColumn = resolveMatchColumn(rel);

        StringJoiner in = new StringJoiner(", ", "(", ")");
        for (Object k : parentKeys) {
            in.add("?");
            params.add(k);
        }

        String where = " WHERE " + childTable + "." + matchColumn + " IN " + in;

        String extra = buildWhere(childEntity, childTable, include.getFilter(), params);
        if (!extra.isBlank()) {
            where = where + " AND " + extra.substring(" WHERE ".length());
        }

        String order = buildOrderBy(childEntity, childTable, include.getOrderBy());
        String paging = buildPaging(include.getLimit(), include.getOffset(), params);

        return new SqlPlan(select + " FROM " + childTable + where + order + paging, params);
    }

    private static String resolveMatchColumn(RelationSchema relationSchema) throws InvalidQueryException {
        String relType = relationSchema.getType() == null ? "" : relationSchema.getType().trim().toLowerCase(Locale.ROOT);

        if (relType.equals("one-to-many") || relType.equals("many-to-one")) {
            String jc = relationSchema.getForeignKey();
            if (jc == null || jc.isBlank()) {
                throw new InvalidQueryException("Relation joinColumn is missing for target: " + relationSchema.getTarget(),
                        HttpStatus.BAD_REQUEST);
            }
            return jc;
        }

        throw new InvalidQueryException("Unsupported relation type: " + relationSchema.getType(), HttpStatus.BAD_REQUEST);
    }
}