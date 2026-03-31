package com.example.sql;

import lombok.Data;

import java.util.List;

@Data
public class SqlPlan {
    private final String sql;
    private final List<Object> params;

    public SqlPlan(String sql, List<Object> params) {
        this.sql = sql;
        this.params = params;
    }
}