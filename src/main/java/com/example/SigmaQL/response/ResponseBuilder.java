package com.example.SigmaQL.response;

import com.example.SigmaQL.common.exceptions.InvalidQueryException;
import com.example.SigmaQL.common.exceptions.UnknownFieldException;
import com.example.SigmaQL.dtos.req.IncludeReqDTO;
import com.example.SigmaQL.registry.RelationSchema;
import com.example.SigmaQL.registry.SchemaRegistry;
import com.example.SigmaQL.sql.SqlBuilder;
import com.example.SigmaQL.sql.SqlPlan;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ResponseBuilder {

    private static final int MAX_INCLUDE_DEPTH = 5;
    private static final int MAX_IN_SIZE = 1000;

    private final JdbcTemplate jdbcTemplate;
    private final SqlBuilder sqlBuilder;
    private final SchemaRegistry schemaRegistry;

    public ResponseBuilder(JdbcTemplate jdbcTemplate, SqlBuilder sqlBuilder, SchemaRegistry schemaRegistry) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlBuilder = sqlBuilder;
        this.schemaRegistry = schemaRegistry;
    }

    public List<Map<String, Object>> applyIncludes(
            String entity,
            List<Map<String, Object>> rows,
            Map<String, IncludeReqDTO> include
    ) throws InvalidQueryException, UnknownFieldException {
        if (include == null || include.isEmpty()) return rows;
        return applyIncludesRecursive(entity, rows, include, 1);
    }

    private List<Map<String, Object>> applyIncludesRecursive(
            String entity,
            List<Map<String, Object>> rows,
            Map<String, IncludeReqDTO> include,
            int depth
    ) throws InvalidQueryException, UnknownFieldException {
        if (include == null || include.isEmpty()) return rows;

        if (depth > MAX_INCLUDE_DEPTH) {
            throw new InvalidQueryException("Include depth exceeded (" + MAX_INCLUDE_DEPTH + ")", HttpStatus.BAD_REQUEST);
        }

        for (Map.Entry<String, IncludeReqDTO> inc : include.entrySet()) {
            String relationName = inc.getKey();
            IncludeReqDTO childReq = inc.getValue();
            if (relationName == null || relationName.isBlank() || childReq == null) continue;

            RelationSchema rel = schemaRegistry.getRelation(entity, relationName);
            String relType = safeLower(rel.getType());

            if (relType.equals("one-to-many")) {
                applyOneToMany(entity, rows, relationName, rel, childReq, depth);
            } else if (relType.equals("many-to-one")) {
                applyManyToOne(entity, rows, relationName, rel, childReq, depth);
            } else {
                throw new InvalidQueryException("Unsupported relation type: " + rel.getType(), HttpStatus.BAD_REQUEST);
            }
        }

        return rows;
    }

    private void applyOneToMany(
            String parentEntity,
            List<Map<String, Object>> parentRows,
            String relationName,
            RelationSchema rel,
            IncludeReqDTO childReq,
            int depth
    ) throws InvalidQueryException, UnknownFieldException {
        String parentPk = schemaRegistry.getPrimaryKey(parentEntity);

        LinkedHashSet<Object> parentIds = new LinkedHashSet<>();
        for (Map<String, Object> r : parentRows) {
            if (r == null) continue;
            Object id = r.get(parentPk);
            if (id != null) parentIds.add(id);
        }

        if (parentIds.isEmpty()) {
            for (Map<String, Object> r : parentRows) r.put(relationName, new ArrayList<>());
            return;
        }

        if (parentIds.size() > MAX_IN_SIZE) {
            throw new InvalidQueryException("Too many parent keys for include: " + parentIds.size(), HttpStatus.BAD_REQUEST);
        }

        SqlPlan plan = sqlBuilder.buildIncludePlan(parentEntity, rel, parentIds, childReq);
        List<Map<String, Object>> children = jdbcTemplate.queryForList(plan.getSql(), plan.getParams().toArray());

        String childFk = rel.getForeignKey();

        Map<Object, List<Map<String, Object>>> grouped = new HashMap<>();
        for (Map<String, Object> childRow : children) {
            if (childRow == null) continue;
            Object fkVal = childRow.get(childFk);
            if (fkVal == null) continue;
            grouped.computeIfAbsent(fkVal, k -> new ArrayList<>()).add(childRow);
        }

        String childEntity = rel.getTarget();
        if (childReq.getInclude() != null && !childReq.getInclude().isEmpty()) {
            for (Map.Entry<Object, List<Map<String, Object>>> e : grouped.entrySet()) {
                List<Map<String, Object>> list = e.getValue();
                applyIncludesRecursive(childEntity, list, childReq.getInclude(), depth + 1);
            }
        }

        for (Map<String, Object> r : parentRows) {
            Object id = r.get(parentPk);
            List<Map<String, Object>> list = (id == null) ? new ArrayList<>() : grouped.getOrDefault(id, new ArrayList<>());
            r.put(relationName, list);
        }
    }

    private void applyManyToOne(
            String parentEntity,
            List<Map<String, Object>> parentRows,
            String relationName,
            RelationSchema rel,
            IncludeReqDTO childReq,
            int depth
    ) throws InvalidQueryException, UnknownFieldException {
        String parentLocalKey = rel.getLocalKey();

        LinkedHashSet<Object> fkValues = new LinkedHashSet<>();
        for (Map<String, Object> r : parentRows) {
            if (r == null) continue;
            Object fk = r.get(parentLocalKey);
            if (fk != null) fkValues.add(fk);
        }

        if (fkValues.isEmpty()) {
            for (Map<String, Object> r : parentRows) r.put(relationName, null);
            return;
        }

        if (fkValues.size() > MAX_IN_SIZE) {
            throw new InvalidQueryException("Too many keys for include: " + fkValues.size(), HttpStatus.BAD_REQUEST);
        }

        SqlPlan plan = sqlBuilder.buildIncludePlan(parentEntity, rel, fkValues, childReq);
        List<Map<String, Object>> children = jdbcTemplate.queryForList(plan.getSql(), plan.getParams().toArray());

        String childPk = rel.getForeignKey();

        Map<Object, Map<String, Object>> byPk = new HashMap<>();
        for (Map<String, Object> childRow : children) {
            if (childRow == null) continue;
            Object pk = childRow.get(childPk);
            if (pk != null) byPk.put(pk, childRow);
        }

        String childEntity = rel.getTarget();
        if (childReq.getInclude() != null && !childReq.getInclude().isEmpty()) {
            List<Map<String, Object>> childList = new ArrayList<>(byPk.values());
            applyIncludesRecursive(childEntity, childList, childReq.getInclude(), depth + 1);
        }

        for (Map<String, Object> r : parentRows) {
            Object fk = r.get(parentLocalKey);
            r.put(relationName, fk == null ? null : byPk.get(fk));
        }
    }

    private String safeLower(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }
}