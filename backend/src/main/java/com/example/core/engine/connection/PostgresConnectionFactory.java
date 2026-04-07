package com.example.core.engine.connection;

import org.springframework.stereotype.Component;

@Component
public class PostgresConnectionFactory {

    public String buildJdbcUrl(PostgresRuntimeConnectionDefinition definition) {
        return "jdbc:postgresql://"
                + definition.host()
                + ":"
                + definition.port()
                + "/"
                + definition.databaseName();
    }
}
