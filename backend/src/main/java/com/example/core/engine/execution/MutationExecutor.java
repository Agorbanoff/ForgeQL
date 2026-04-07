package com.example.core.engine.execution;

import com.example.core.engine.connection.PostgresRuntimeConnectionDefinition;
import com.example.core.engine.mutation.MutationResult;
import com.example.core.engine.sql.SqlCommand;
import org.springframework.stereotype.Component;

@Component
public class MutationExecutor {

    public MutationResult execute(PostgresRuntimeConnectionDefinition definition, SqlCommand sqlCommand) {
        throw new UnsupportedOperationException("Mutation execution is not implemented yet for datasource " + definition.datasourceId());
    }
}
