package com.example.core.engine.execution;

import com.example.core.engine.connection.PostgresRuntimeConnectionDefinition;
import com.example.core.engine.sql.SqlCommand;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AggregateExecutor {

    public Map<String, Object> execute(PostgresRuntimeConnectionDefinition definition, SqlCommand sqlCommand) {
        throw new UnsupportedOperationException("Aggregate execution is not implemented yet for datasource " + definition.datasourceId());
    }
}
