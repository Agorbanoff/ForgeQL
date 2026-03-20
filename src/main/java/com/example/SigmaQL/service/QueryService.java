package com.example.SigmaQL.service;

import com.example.SigmaQL.common.exceptions.InvalidQueryException;
import com.example.SigmaQL.common.exceptions.UnknownFieldException;
import com.example.SigmaQL.controller.dtos.req.QueryReqDTO;
import com.example.SigmaQL.response.ResponseBuilder;
import com.example.SigmaQL.sql.SqlBuilder;
import com.example.SigmaQL.sql.SqlPlan;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class QueryService {

    private final JdbcTemplate jdbcTemplate;
    private final SqlBuilder sqlBuilder;
    private final ResponseBuilder responseBuilder;

    public QueryService(JdbcTemplate jdbcTemplate, SqlBuilder sqlBuilder, ResponseBuilder responseBuilder) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlBuilder = sqlBuilder;
        this.responseBuilder = responseBuilder;
    }

    public List<Map<String, Object>> execute(QueryReqDTO dto) throws InvalidQueryException, UnknownFieldException {
        SqlPlan plan = sqlBuilder.build(dto);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(plan.getSql(), plan.getParams().toArray());
        return responseBuilder.applyIncludes(dto.getEntity(), rows, dto.getInclude());
    }
}