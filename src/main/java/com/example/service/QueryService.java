package com.example.service;

import com.example.common.exceptions.InvalidQueryException;
import com.example.common.exceptions.UnknownFieldException;
import com.example.controller.dtos.request.QueryReqDTO;
import com.example.response.ResponseBuilder;
import com.example.sql.SqlBuilder;
import com.example.sql.SqlPlan;
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