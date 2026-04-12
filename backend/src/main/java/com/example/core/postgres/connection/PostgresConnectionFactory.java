package com.example.core.postgres.connection;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;

import org.springframework.stereotype.Component;

@Component
public class PostgresConnectionFactory {

    private final PostgresConnectionPropertiesFactory connectionPropertiesFactory;
    private final PostgresConnectionValidator connectionValidator;
    private final PostgresRuntimePoolManager runtimePoolManager;

    public PostgresConnectionFactory(
            PostgresConnectionPropertiesFactory connectionPropertiesFactory,
            PostgresConnectionValidator connectionValidator,
            PostgresRuntimePoolManager runtimePoolManager
    ) {
        this.connectionPropertiesFactory = connectionPropertiesFactory;
        this.connectionValidator = connectionValidator;
        this.runtimePoolManager = runtimePoolManager;
    }

    public String buildJdbcUrl(PostgresRuntimeConnectionDefinition definition) {
        return "jdbc:postgresql://"
                + formatHost(definition.host())
                + ":"
                + definition.port()
                + "/"
                + encodeDatabaseName(definition.databaseName());
    }

    public PostgresJdbcConnectionConfig createConfig(PostgresRuntimeConnectionDefinition definition) {
        connectionValidator.validateRuntimeDefinition(definition);
        return new PostgresJdbcConnectionConfig(
                buildJdbcUrl(definition),
                connectionPropertiesFactory.create(definition)
        );
    }

    public Connection openConnection(PostgresRuntimeConnectionDefinition definition) throws SQLException {
        PostgresJdbcConnectionConfig config = createConfig(definition);
        return runtimePoolManager.openConnection(definition, config);
    }

    private String formatHost(String host) {
        if (host != null && host.contains(":") && !host.startsWith("[") && !host.endsWith("]")) {
            return "[" + host + "]";
        }
        return host;
    }

    private String encodeDatabaseName(String databaseName) {
        return URLEncoder.encode(databaseName, StandardCharsets.UTF_8).replace("+", "%20");
    }
}

