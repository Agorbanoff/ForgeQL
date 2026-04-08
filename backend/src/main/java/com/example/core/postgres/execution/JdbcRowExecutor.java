package com.example.core.postgres.execution;

import com.example.core.postgres.connection.PostgresRuntimeConnectionDefinition;
import com.example.core.postgres.sql.SqlCommand;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class JdbcRowExecutor {

    public List<Map<String, Object>> execute(PostgresRuntimeConnectionDefinition definition, SqlCommand sqlCommand) {
        throw new UnsupportedOperationException("JDBC row execution is not implemented yet for datasource " + definition.datasourceId());
    }
}

