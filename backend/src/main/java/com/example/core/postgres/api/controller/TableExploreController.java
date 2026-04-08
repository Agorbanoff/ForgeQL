package com.example.core.postgres.api.controller;

import com.example.core.postgres.execution.JdbcRowExecutor;
import com.example.core.postgres.plan.ReadQueryPlanner;
import com.example.core.postgres.sql.ReadSqlBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/core/datasources/{datasourceId}/tables")
public class TableExploreController {

    private final ReadQueryPlanner readQueryPlanner;
    private final ReadSqlBuilder readSqlBuilder;
    private final JdbcRowExecutor jdbcRowExecutor;

    @Autowired
    public TableExploreController(
            ReadQueryPlanner readQueryPlanner,
            ReadSqlBuilder readSqlBuilder,
            JdbcRowExecutor jdbcRowExecutor
    ) {
        this.readQueryPlanner = readQueryPlanner;
        this.readSqlBuilder = readSqlBuilder;
        this.jdbcRowExecutor = jdbcRowExecutor;
    }
}

