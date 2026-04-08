package com.example.core.postgres.connection;

import com.example.common.exceptions.InvalidDataSourceConfigurationException;
import com.example.common.exceptions.PostgresTargetMismatchException;
import com.example.persistence.Enums.SslMode;
import org.springframework.stereotype.Component;

@Component
public class PostgresConnectionValidator {

    public void validateRuntimeDefinition(PostgresRuntimeConnectionDefinition definition) {
        require(definition.host(), "Datasource host is required");
        require(definition.databaseName(), "Datasource database name is required");
        require(definition.schemaName(), "Datasource schema name is required");
        require(definition.username(), "Datasource username is required");
        require(definition.password(), "Datasource password is required");

        if (definition.sslMode() == null) {
            throw new InvalidDataSourceConfigurationException("Datasource SSL mode is required");
        }
        if (definition.port() == null || definition.port() < 1 || definition.port() > 65535) {
            throw new InvalidDataSourceConfigurationException("Datasource port must be between 1 and 65535");
        }
        if ((definition.sslMode() == SslMode.VERIFY_CA
                || definition.sslMode() == SslMode.VERIFY_FULL)
                && (definition.sslRootCertPath() == null || definition.sslRootCertPath().isBlank())) {
            throw new InvalidDataSourceConfigurationException(
                    "sslRootCertRef is required for PostgreSQL SSL mode " + definition.sslMode()
            );
        }
    }

    public void validateProductName(String databaseProductName) {
        if (databaseProductName == null || !"PostgreSQL".equalsIgnoreCase(databaseProductName)) {
            throw new PostgresTargetMismatchException("Target datasource is not PostgreSQL");
        }
    }

    private void require(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new InvalidDataSourceConfigurationException(message);
        }
    }
}

