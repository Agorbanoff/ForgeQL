package com.example.core.postgres.execution;

import com.example.core.postgres.api.dto.request.AggregateRequest;
import com.example.core.postgres.api.dto.response.AggregateResponse;
import com.example.core.postgres.ast.AggregateAst;
import com.example.core.postgres.ast.AggregateQueryAstBuilder;
import com.example.core.postgres.connection.PostgresRuntimeConnectionDefinition;
import com.example.core.postgres.connection.PostgresRuntimeConnectionResolver;
import com.example.core.postgres.plan.AggregateExecutionPlan;
import com.example.core.postgres.plan.AggregateQueryPlanner;
import com.example.core.postgres.schema.ResolvedTableIdentifier;
import com.example.core.postgres.schema.SchemaReadService;
import com.example.core.postgres.sql.AggregateSqlBuilder;
import com.example.core.postgres.sql.SqlCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AggregateQueryService {

    private final SchemaReadService schemaReadService;
    private final AggregateQueryAstBuilder aggregateQueryAstBuilder;
    private final AggregateQueryPlanner aggregateQueryPlanner;
    private final AggregateSqlBuilder aggregateSqlBuilder;
    private final AggregateExecutor aggregateExecutor;
    private final PostgresRuntimeConnectionResolver runtimeConnectionResolver;

    @Autowired
    public AggregateQueryService(
            SchemaReadService schemaReadService,
            AggregateQueryAstBuilder aggregateQueryAstBuilder,
            AggregateQueryPlanner aggregateQueryPlanner,
            AggregateSqlBuilder aggregateSqlBuilder,
            AggregateExecutor aggregateExecutor,
            PostgresRuntimeConnectionResolver runtimeConnectionResolver
    ) {
        this.schemaReadService = schemaReadService;
        this.aggregateQueryAstBuilder = aggregateQueryAstBuilder;
        this.aggregateQueryPlanner = aggregateQueryPlanner;
        this.aggregateSqlBuilder = aggregateSqlBuilder;
        this.aggregateExecutor = aggregateExecutor;
        this.runtimeConnectionResolver = runtimeConnectionResolver;
    }

    public AggregateResponse aggregate(
            Integer datasourceId,
            Integer userId,
            String tableIdentifier,
            AggregateRequest request
    ) {
        ResolvedTableIdentifier resolvedTableIdentifier = schemaReadService.resolveTableIdentifier(
                datasourceId,
                userId,
                tableIdentifier
        );
        AggregateAst aggregateAst = aggregateQueryAstBuilder.build(resolvedTableIdentifier.qualifiedName(), request);
        AggregateExecutionPlan executionPlan = aggregateQueryPlanner.plan(datasourceId, userId, aggregateAst);
        SqlCommand sqlCommand = aggregateSqlBuilder.build(executionPlan);
        PostgresRuntimeConnectionDefinition connectionDefinition = runtimeConnectionResolver.resolve(datasourceId, userId);
        return aggregateExecutor.execute(connectionDefinition, sqlCommand);
    }
}
