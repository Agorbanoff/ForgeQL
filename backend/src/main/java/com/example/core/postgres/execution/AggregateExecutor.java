package com.example.core.postgres.execution;

import com.example.core.postgres.api.dto.response.AggregateResponse;
import com.example.core.postgres.api.dto.response.AggregateRowResponse;
import com.example.core.postgres.connection.PostgresRuntimeConnectionDefinition;
import com.example.core.postgres.sql.SqlCommand;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AggregateExecutor {

    private final JdbcRowExecutor jdbcRowExecutor;

    public AggregateExecutor(JdbcRowExecutor jdbcRowExecutor) {
        this.jdbcRowExecutor = jdbcRowExecutor;
    }

    public AggregateResponse execute(PostgresRuntimeConnectionDefinition definition, SqlCommand sqlCommand) {
        List<Map<String, Object>> rows = jdbcRowExecutor.execute(definition, sqlCommand);
        return new AggregateResponse(
                rows.stream()
                        .map(AggregateRowResponse::new)
                        .toList()
        );
    }
}

