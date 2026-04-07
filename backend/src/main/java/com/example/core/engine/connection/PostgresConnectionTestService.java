package com.example.core.engine.connection;

import org.springframework.stereotype.Service;

@Service
public class PostgresConnectionTestService {

    private final PostgresRuntimeConnectionResolver runtimeConnectionResolver;
    private final PostgresConnectionFactory connectionFactory;

    public PostgresConnectionTestService(
            PostgresRuntimeConnectionResolver runtimeConnectionResolver,
            PostgresConnectionFactory connectionFactory
    ) {
        this.runtimeConnectionResolver = runtimeConnectionResolver;
        this.connectionFactory = connectionFactory;
    }

    public PostgresConnectionTestResult test(Integer datasourceId, Integer userId) {
        PostgresRuntimeConnectionDefinition definition = runtimeConnectionResolver.resolve(datasourceId, userId);
        String jdbcUrl = connectionFactory.buildJdbcUrl(definition);
        throw new UnsupportedOperationException("Connection testing is not implemented yet for " + jdbcUrl);
    }
}
