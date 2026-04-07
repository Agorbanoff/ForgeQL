package com.example.core.engine.execution;

import com.example.core.engine.connection.PostgresRuntimeConnectionDefinition;
import com.example.core.engine.sql.SqlCommand;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class JdbcRowExecutor {

    public List<Map<String, Object>> execute(PostgresRuntimeConnectionDefinition definition, SqlCommand sqlCommand) {
        throw new UnsupportedOperationException("JDBC row execution is not implemented yet for datasource " + definition.datasourceId());
    }
}
