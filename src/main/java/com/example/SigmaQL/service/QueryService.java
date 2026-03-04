package com.example.SigmaQL.service;

import com.example.SigmaQL.common.exceptions.InvalidQueryException;
import com.example.SigmaQL.dtos.req.QueryReqDTO;
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

    public QueryService(JdbcTemplate jdbcTemplate, SqlBuilder sqlBuilder) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlBuilder = sqlBuilder;
    }

    public List<Map<String, Object>> execute(QueryReqDTO dto) throws InvalidQueryException {
        SqlPlan plan = sqlBuilder.build(dto);
        return jdbcTemplate.queryForList(plan.getSql(), plan.getParams().toArray());
    }
}