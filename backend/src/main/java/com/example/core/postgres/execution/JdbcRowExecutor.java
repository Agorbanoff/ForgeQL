package com.example.core.postgres.execution;

import com.example.common.exceptions.InvalidDataSourceConfigurationException;
import com.example.common.exceptions.InvalidExecutionPlanException;
import com.example.common.exceptions.PostgresQueryExecutionException;
import com.example.core.postgres.connection.PostgresConnectionFactory;
import com.example.core.postgres.connection.PostgresRuntimeConnectionDefinition;
import com.example.core.postgres.sql.SqlCommand;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class JdbcRowExecutor {

    private final PostgresConnectionFactory connectionFactory;

    public JdbcRowExecutor(PostgresConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public List<Map<String, Object>> execute(PostgresRuntimeConnectionDefinition definition, SqlCommand sqlCommand) {
        validateExecutionInputs(definition, sqlCommand);

        try (Connection connection = connectionFactory.openConnection(definition);
             PreparedStatement preparedStatement = connection.prepareStatement(sqlCommand.sql())) {
            bindParameters(preparedStatement, sqlCommand.parameters());

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return readRows(resultSet);
            }
        } catch (SQLException e) {
            throw new PostgresQueryExecutionException(
                    "Failed to execute PostgreSQL read query for datasource " + definition.datasourceId(),
                    e
            );
        }
    }

    private void validateExecutionInputs(PostgresRuntimeConnectionDefinition definition, SqlCommand sqlCommand) {
        if (definition == null) {
            throw new InvalidDataSourceConfigurationException("Runtime connection definition is required");
        }
        if (sqlCommand == null) {
            throw new InvalidExecutionPlanException("SQL command is required");
        }
        if (sqlCommand.sql() == null || sqlCommand.sql().isBlank()) {
            throw new InvalidExecutionPlanException("SQL command text is required");
        }
    }

    private void bindParameters(PreparedStatement preparedStatement, List<Object> parameters) throws SQLException {
        for (int index = 0; index < parameters.size(); index++) {
            preparedStatement.setObject(index + 1, parameters.get(index));
        }
    }

    private List<Map<String, Object>> readRows(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metadata = resultSet.getMetaData();
        int columnCount = metadata.getColumnCount();
        List<Map<String, Object>> rows = new ArrayList<>();

        while (resultSet.next()) {
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
                row.put(metadata.getColumnLabel(columnIndex), resultSet.getObject(columnIndex));
            }
            rows.add(row);
        }

        return List.copyOf(rows);
    }
}
