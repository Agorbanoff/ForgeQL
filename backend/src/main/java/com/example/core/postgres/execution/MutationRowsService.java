package com.example.core.postgres.execution;

import com.example.common.exceptions.RowNotFoundException;
import com.example.core.postgres.api.dto.request.CreateRowRequest;
import com.example.core.postgres.api.dto.request.UpdateRowRequest;
import com.example.core.postgres.api.dto.response.CreateRowResponse;
import com.example.core.postgres.api.dto.response.DeleteRowResponse;
import com.example.core.postgres.api.dto.response.UpdateRowResponse;
import com.example.core.postgres.ast.DeleteMutationAst;
import com.example.core.postgres.ast.InsertMutationAst;
import com.example.core.postgres.ast.MutationAstBuilder;
import com.example.core.postgres.ast.UpdateMutationAst;
import com.example.core.postgres.connection.PostgresRuntimeConnectionDefinition;
import com.example.core.postgres.connection.PostgresRuntimeConnectionResolver;
import com.example.core.postgres.mutation.MutationResult;
import com.example.core.postgres.plan.DeleteExecutionPlan;
import com.example.core.postgres.plan.DeletePlanner;
import com.example.core.postgres.plan.InsertExecutionPlan;
import com.example.core.postgres.plan.InsertPlanner;
import com.example.core.postgres.plan.UpdateExecutionPlan;
import com.example.core.postgres.plan.UpdatePlanner;
import com.example.core.postgres.schema.ResolvedTableIdentifier;
import com.example.core.postgres.schema.SchemaReadService;
import com.example.core.postgres.sql.DeleteSqlBuilder;
import com.example.core.postgres.sql.InsertSqlBuilder;
import com.example.core.postgres.sql.SqlCommand;
import com.example.core.postgres.sql.UpdateSqlBuilder;
import org.springframework.stereotype.Service;

@Service
public class MutationRowsService {

    private final SchemaReadService schemaReadService;
    private final MutationAstBuilder mutationAstBuilder;
    private final InsertPlanner insertPlanner;
    private final UpdatePlanner updatePlanner;
    private final DeletePlanner deletePlanner;
    private final InsertSqlBuilder insertSqlBuilder;
    private final UpdateSqlBuilder updateSqlBuilder;
    private final DeleteSqlBuilder deleteSqlBuilder;
    private final MutationExecutor mutationExecutor;
    private final PostgresRuntimeConnectionResolver runtimeConnectionResolver;

    public MutationRowsService(
            SchemaReadService schemaReadService,
            MutationAstBuilder mutationAstBuilder,
            InsertPlanner insertPlanner,
            UpdatePlanner updatePlanner,
            DeletePlanner deletePlanner,
            InsertSqlBuilder insertSqlBuilder,
            UpdateSqlBuilder updateSqlBuilder,
            DeleteSqlBuilder deleteSqlBuilder,
            MutationExecutor mutationExecutor,
            PostgresRuntimeConnectionResolver runtimeConnectionResolver
    ) {
        this.schemaReadService = schemaReadService;
        this.mutationAstBuilder = mutationAstBuilder;
        this.insertPlanner = insertPlanner;
        this.updatePlanner = updatePlanner;
        this.deletePlanner = deletePlanner;
        this.insertSqlBuilder = insertSqlBuilder;
        this.updateSqlBuilder = updateSqlBuilder;
        this.deleteSqlBuilder = deleteSqlBuilder;
        this.mutationExecutor = mutationExecutor;
        this.runtimeConnectionResolver = runtimeConnectionResolver;
    }

    public CreateRowResponse createRow(
            Integer datasourceId,
            Integer userId,
            String tableIdentifier,
            CreateRowRequest request
    ) {
        ResolvedTableIdentifier resolvedTableIdentifier = schemaReadService.resolveTableIdentifier(
                datasourceId,
                userId,
                tableIdentifier
        );
        InsertMutationAst mutationAst = mutationAstBuilder.buildInsert(resolvedTableIdentifier.qualifiedName(), request);
        InsertExecutionPlan executionPlan = insertPlanner.plan(datasourceId, userId, mutationAst);
        SqlCommand sqlCommand = insertSqlBuilder.build(executionPlan);
        PostgresRuntimeConnectionDefinition connectionDefinition = runtimeConnectionResolver.resolve(datasourceId, userId);
        MutationResult mutationResult = mutationExecutor.execute(connectionDefinition, executionPlan, sqlCommand);

        return new CreateRowResponse(
                mutationResult.affectedRows(),
                mutationResult.identity(),
                mutationResult.returnedRow()
        );
    }

    public UpdateRowResponse updateRow(
            Integer datasourceId,
            Integer userId,
            String tableIdentifier,
            Long primaryKeyValue,
            UpdateRowRequest request
    ) {
        ResolvedTableIdentifier resolvedTableIdentifier = schemaReadService.resolveTableIdentifier(
                datasourceId,
                userId,
                tableIdentifier
        );
        UpdateMutationAst mutationAst = mutationAstBuilder.buildUpdate(
                resolvedTableIdentifier.qualifiedName(),
                primaryKeyValue,
                request
        );
        UpdateExecutionPlan executionPlan = updatePlanner.plan(datasourceId, userId, mutationAst);
        SqlCommand sqlCommand = updateSqlBuilder.build(executionPlan);
        PostgresRuntimeConnectionDefinition connectionDefinition = runtimeConnectionResolver.resolve(datasourceId, userId);
        MutationResult mutationResult = mutationExecutor.execute(connectionDefinition, executionPlan, sqlCommand);
        if (mutationResult.affectedRows() == 0) {
            throw new RowNotFoundException("Row not found for the requested primary key");
        }

        return new UpdateRowResponse(
                mutationResult.affectedRows(),
                mutationResult.returnedRow()
        );
    }

    public DeleteRowResponse deleteRow(
            Integer datasourceId,
            Integer userId,
            String tableIdentifier,
            Long primaryKeyValue
    ) {
        ResolvedTableIdentifier resolvedTableIdentifier = schemaReadService.resolveTableIdentifier(
                datasourceId,
                userId,
                tableIdentifier
        );
        DeleteMutationAst mutationAst = mutationAstBuilder.buildDelete(
                resolvedTableIdentifier.qualifiedName(),
                primaryKeyValue
        );
        DeleteExecutionPlan executionPlan = deletePlanner.plan(datasourceId, userId, mutationAst);
        SqlCommand sqlCommand = deleteSqlBuilder.build(executionPlan);
        PostgresRuntimeConnectionDefinition connectionDefinition = runtimeConnectionResolver.resolve(datasourceId, userId);
        MutationResult mutationResult = mutationExecutor.execute(connectionDefinition, executionPlan, sqlCommand);
        if (mutationResult.affectedRows() == 0) {
            throw new RowNotFoundException("Row not found for the requested primary key");
        }

        return new DeleteRowResponse(
                mutationResult.affectedRows(),
                mutationResult.identity()
        );
    }
}
