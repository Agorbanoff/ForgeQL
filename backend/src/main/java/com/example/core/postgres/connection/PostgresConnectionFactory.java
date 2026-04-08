package com.example.core.postgres.connection;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.springframework.stereotype.Component;

@Component
public class PostgresConnectionFactory {

    private final PostgresConnectionPropertiesFactory connectionPropertiesFactory;
    private final PostgresConnectionValidator connectionValidator;

    public PostgresConnectionFactory(
            PostgresConnectionPropertiesFactory connectionPropertiesFactory,
            PostgresConnectionValidator connectionValidator
    ) {
        this.connectionPropertiesFactory = connectionPropertiesFactory;
        this.connectionValidator = connectionValidator;
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
        return openConnection(config);
    }

    public Connection openConnection(PostgresJdbcConnectionConfig config) throws SQLException {
        return DriverManager.getConnection(config.jdbcUrl(), config.properties());
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

