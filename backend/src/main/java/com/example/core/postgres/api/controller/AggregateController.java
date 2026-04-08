package com.example.core.postgres.api.controller;

import com.example.core.postgres.execution.AggregateExecutor;
import com.example.core.postgres.plan.AggregateQueryPlanner;
import com.example.core.postgres.sql.AggregateSqlBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/core/datasources/{datasourceId}/tables")
public class AggregateController {

    private final AggregateQueryPlanner aggregateQueryPlanner;
    private final AggregateSqlBuilder aggregateSqlBuilder;
    private final AggregateExecutor aggregateExecutor;

    @Autowired
    public AggregateController(
            AggregateQueryPlanner aggregateQueryPlanner,
            AggregateSqlBuilder aggregateSqlBuilder,
            AggregateExecutor aggregateExecutor
    ) {
        this.aggregateQueryPlanner = aggregateQueryPlanner;
        this.aggregateSqlBuilder = aggregateSqlBuilder;
        this.aggregateExecutor = aggregateExecutor;
    }
}

