package com.example.core.engine.connection;

import com.example.persistence.Enums.SslMode;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Properties;

@Component
public class PostgresConnectionPropertiesFactory {

    public Properties create(PostgresRuntimeConnectionDefinition definition) {
        Properties properties = new Properties();
        properties.setProperty("user", definition.username());
        properties.setProperty("password", definition.password());

        if (definition.sslMode() != null) {
            properties.setProperty("sslmode", toDriverValue(definition.sslMode()));
        }
        if (definition.connectTimeoutMs() != null) {
            properties.setProperty("connectTimeout", String.valueOf(Math.max(1, definition.connectTimeoutMs() / 1000)));
        }
        if (definition.socketTimeoutMs() != null) {
            properties.setProperty("socketTimeout", String.valueOf(Math.max(1, definition.socketTimeoutMs() / 1000)));
        }
        if (definition.applicationName() != null) {
            properties.setProperty("ApplicationName", definition.applicationName());
        }
        if (definition.sslRootCertRef() != null) {
            properties.setProperty("sslrootcert", definition.sslRootCertRef());
        }

        return properties;
    }

    private String toDriverValue(SslMode sslMode) {
        return sslMode.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
