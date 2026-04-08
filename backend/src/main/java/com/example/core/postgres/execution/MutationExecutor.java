package com.example.core.postgres.execution;

import com.example.core.postgres.connection.PostgresRuntimeConnectionDefinition;
import com.example.core.postgres.mutation.MutationResult;
import com.example.core.postgres.sql.SqlCommand;
import org.springframework.stereotype.Component;

@Component
public class MutationExecutor {

    public MutationResult execute(PostgresRuntimeConnectionDefinition definition, SqlCommand sqlCommand) {
        throw new UnsupportedOperationException("Mutation execution is not implemented yet for datasource " + definition.datasourceId());
    }
}

