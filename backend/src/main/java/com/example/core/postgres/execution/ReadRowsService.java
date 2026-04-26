package com.example.core.postgres.execution;

import com.example.core.postgres.api.dto.request.ReadRowsRequest;
import com.example.core.postgres.api.dto.response.RowsPageResponse;
import com.example.core.postgres.api.dto.response.RowsResponse;
import com.example.core.postgres.ast.ReadQueryAstBuilder;
import com.example.core.postgres.ast.ReadTableAst;
import com.example.core.postgres.connection.PostgresRuntimeConnectionDefinition;
import com.example.core.postgres.connection.PostgresRuntimeConnectionResolver;
import com.example.core.postgres.plan.ReadExecutionPlan;
import com.example.core.postgres.plan.ReadQueryPlanner;
import com.example.core.postgres.schema.ResolvedTableIdentifier;
import com.example.core.postgres.schema.SchemaReadService;
import com.example.core.postgres.sql.ReadSqlBuilder;
import com.example.core.postgres.sql.SqlCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ReadRowsService {

    private final SchemaReadService schemaReadService;
    private final ReadQueryAstBuilder readQueryAstBuilder;
    private final ReadQueryPlanner readQueryPlanner;
    private final ReadSqlBuilder readSqlBuilder;
    private final JdbcRowExecutor jdbcRowExecutor;
    private final PostgresRuntimeConnectionResolver runtimeConnectionResolver;

    @Autowired
    public ReadRowsService(
            SchemaReadService schemaReadService,
            ReadQueryAstBuilder readQueryAstBuilder,
            ReadQueryPlanner readQueryPlanner,
            ReadSqlBuilder readSqlBuilder,
            JdbcRowExecutor jdbcRowExecutor,
            PostgresRuntimeConnectionResolver runtimeConnectionResolver
    ) {
        this.schemaReadService = schemaReadService;
        this.readQueryAstBuilder = readQueryAstBuilder;
        this.readQueryPlanner = readQueryPlanner;
        this.readSqlBuilder = readSqlBuilder;
        this.jdbcRowExecutor = jdbcRowExecutor;
        this.runtimeConnectionResolver = runtimeConnectionResolver;
    }

    public RowsResponse readRows(
            Integer datasourceId,
            Integer userId,
            String tableIdentifier,
            ReadRowsRequest request
    ) {
        ResolvedTableIdentifier resolvedTableIdentifier = schemaReadService.resolveTableIdentifier(
                datasourceId,
                userId,
                tableIdentifier
        );
        ReadTableAst readTableAst = readQueryAstBuilder.build(resolvedTableIdentifier.qualifiedName(), request);
        ReadExecutionPlan executionPlan = readQueryPlanner.plan(datasourceId, userId, readTableAst);
        SqlCommand sqlCommand = readSqlBuilder.build(executionPlan);
        PostgresRuntimeConnectionDefinition connectionDefinition = runtimeConnectionResolver.resolve(datasourceId);
        List<Map<String, Object>> rows = jdbcRowExecutor.execute(connectionDefinition, sqlCommand);

        return new RowsResponse(
                rows,
                new RowsPageResponse(
                        rows.size(),
                        executionPlan.pagination().limit(),
                        executionPlan.pagination().offset()
                )
        );
    }
}
