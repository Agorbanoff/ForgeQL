package com.example.core.postgres.execution;

import com.example.common.exceptions.InvalidDataSourceConfigurationException;
import com.example.common.exceptions.InvalidExecutionPlanException;
import com.example.common.exceptions.PostgresMutationExecutionException;
import com.example.core.postgres.connection.PostgresConnectionFactory;
import com.example.core.postgres.connection.PostgresRuntimeConnectionDefinition;
import com.example.core.postgres.mutation.MutationResult;
import com.example.core.postgres.mutation.MutationType;
import com.example.core.postgres.plan.DeleteExecutionPlan;
import com.example.core.postgres.plan.InsertExecutionPlan;
import com.example.core.postgres.plan.UpdateExecutionPlan;
import com.example.core.postgres.sql.SqlCommand;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class MutationExecutor {

    private final PostgresConnectionFactory connectionFactory;
    private final PreparedStatementParameterBinder parameterBinder;

    public MutationExecutor(
            PostgresConnectionFactory connectionFactory,
            PreparedStatementParameterBinder parameterBinder
    ) {
        this.connectionFactory = connectionFactory;
        this.parameterBinder = parameterBinder;
    }

    public MutationResult execute(
            PostgresRuntimeConnectionDefinition definition,
            InsertExecutionPlan executionPlan,
            SqlCommand sqlCommand
    ) {
        validateExecutionInputs(definition, executionPlan, sqlCommand);
        return executeInTransaction(
                definition,
                MutationType.INSERT,
                executionPlan.primaryKeyColumn(),
                sqlCommand
        );
    }

    public MutationResult execute(
            PostgresRuntimeConnectionDefinition definition,
            UpdateExecutionPlan executionPlan,
            SqlCommand sqlCommand
    ) {
        validateExecutionInputs(definition, executionPlan, sqlCommand);
        return executeInTransaction(
                definition,
                MutationType.UPDATE,
                executionPlan.primaryKeyColumn(),
                sqlCommand
        );
    }

    public MutationResult execute(
            PostgresRuntimeConnectionDefinition definition,
            DeleteExecutionPlan executionPlan,
            SqlCommand sqlCommand
    ) {
        validateExecutionInputs(definition, executionPlan, sqlCommand);
        return executeInTransaction(
                definition,
                MutationType.DELETE,
                executionPlan.primaryKeyColumn(),
                sqlCommand
        );
    }

    private MutationResult executeInTransaction(
            PostgresRuntimeConnectionDefinition definition,
            MutationType mutationType,
            String primaryKeyColumn,
            SqlCommand sqlCommand
    ) {
        try (Connection connection = connectionFactory.openConnection(definition)) {
            connection.setAutoCommit(false);

            try {
                MutationResult mutationResult = executeMutation(connection, definition, mutationType, primaryKeyColumn, sqlCommand);
                connection.commit();
                return mutationResult;
            } catch (PostgresMutationExecutionException e) {
                rollbackQuietly(connection, definition.datasourceId(), e);
                throw e;
            } catch (SQLException e) {
                rollbackQuietly(connection, definition.datasourceId(), e);
                throw new PostgresMutationExecutionException(
                        "Failed to execute PostgreSQL mutation for datasource " + definition.datasourceId(),
                        e
                );
            } catch (RuntimeException e) {
                rollbackQuietly(connection, definition.datasourceId(), e);
                throw new PostgresMutationExecutionException(
                        "Failed to map PostgreSQL mutation result for datasource " + definition.datasourceId(),
                        e
                );
            }
        } catch (SQLException e) {
            throw new PostgresMutationExecutionException(
                    "Failed to open PostgreSQL mutation transaction for datasource " + definition.datasourceId(),
                    e
            );
        }
    }

    private MutationResult executeMutation(
            Connection connection,
            PostgresRuntimeConnectionDefinition definition,
            MutationType mutationType,
            String primaryKeyColumn,
            SqlCommand sqlCommand
    ) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sqlCommand.sql())) {
            try (PreparedStatementParameterBinder.BoundSqlArrayResources ignored =
                         parameterBinder.bind(connection, preparedStatement, sqlCommand.parameters());
                 ResultSet resultSet = preparedStatement.executeQuery()) {
                return mapMutationResult(resultSet, definition, mutationType, primaryKeyColumn);
            }
        }
    }

    private MutationResult mapMutationResult(
            ResultSet resultSet,
            PostgresRuntimeConnectionDefinition definition,
            MutationType mutationType,
            String primaryKeyColumn
    ) throws SQLException {
        Map<String, Object> returnedRow = null;
        long affectedRows = 0L;

        if (resultSet.next()) {
            returnedRow = readRow(resultSet);
            affectedRows = 1L;
        }
        if (resultSet.next()) {
            throw new PostgresMutationExecutionException(
                    "Mutation returned more than one row unexpectedly for datasource " + definition.datasourceId(),
                    new IllegalStateException("Single-row mutation returned more than one row")
            );
        }

        Object identity = returnedRow == null ? null : returnedRow.get(primaryKeyColumn);
        return new MutationResult(mutationType, affectedRows, identity, returnedRow);
    }

    private void validateExecutionInputs(
            PostgresRuntimeConnectionDefinition definition,
            Object executionPlan,
            SqlCommand sqlCommand
    ) {
        if (definition == null) {
            throw new InvalidDataSourceConfigurationException("Runtime connection definition is required");
        }
        if (executionPlan == null) {
            throw new InvalidExecutionPlanException("Mutation execution plan is required");
        }
        if (sqlCommand == null) {
            throw new InvalidExecutionPlanException("SQL command is required");
        }
        if (sqlCommand.sql() == null || sqlCommand.sql().isBlank()) {
            throw new InvalidExecutionPlanException("SQL command text is required");
        }
    }

    private Map<String, Object> readRow(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metadata = resultSet.getMetaData();
        int columnCount = metadata.getColumnCount();
        LinkedHashMap<String, Object> row = new LinkedHashMap<>();

        for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
            row.put(metadata.getColumnLabel(columnIndex), resultSet.getObject(columnIndex));
        }

        return Collections.unmodifiableMap(new LinkedHashMap<>(row));
    }

    private void rollbackQuietly(Connection connection, Integer datasourceId, Exception originalException) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            originalException.addSuppressed(rollbackException);
            PostgresMutationExecutionException rollbackFailure = new PostgresMutationExecutionException(
                    "Failed to roll back PostgreSQL mutation transaction for datasource " + datasourceId,
                    rollbackException
            );
            rollbackFailure.addSuppressed(originalException);
            throw rollbackFailure;
        }
    }
}

