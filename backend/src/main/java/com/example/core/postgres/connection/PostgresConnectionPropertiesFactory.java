package com.example.core.postgres.connection;

import com.example.common.exceptions.InvalidExtraJdbcOptionsException;
import com.example.persistence.Enums.SslMode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

@Component
public class PostgresConnectionPropertiesFactory {

    private static final String PROPERTY_USER = "user";
    private static final String PROPERTY_PASSWORD = "password";
    private static final String PROPERTY_CURRENT_SCHEMA = "currentSchema";
    private static final String PROPERTY_SSL_MODE = "sslmode";
    private static final String PROPERTY_SSL_ROOT_CERT = "sslrootcert";
    private static final String PROPERTY_CONNECT_TIMEOUT = "connectTimeout";
    private static final String PROPERTY_SOCKET_TIMEOUT = "socketTimeout";
    private static final String PROPERTY_APPLICATION_NAME = "ApplicationName";

    private static final Set<String> RESERVED_OPTION_KEYS = Set.of(
            PROPERTY_USER.toLowerCase(Locale.ROOT),
            PROPERTY_PASSWORD.toLowerCase(Locale.ROOT),
            PROPERTY_CURRENT_SCHEMA.toLowerCase(Locale.ROOT),
            PROPERTY_SSL_MODE.toLowerCase(Locale.ROOT),
            PROPERTY_SSL_ROOT_CERT.toLowerCase(Locale.ROOT),
            PROPERTY_CONNECT_TIMEOUT.toLowerCase(Locale.ROOT),
            PROPERTY_SOCKET_TIMEOUT.toLowerCase(Locale.ROOT),
            PROPERTY_APPLICATION_NAME.toLowerCase(Locale.ROOT)
    );

    private final ObjectMapper objectMapper;

    public PostgresConnectionPropertiesFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Properties create(PostgresRuntimeConnectionDefinition definition) {
        Properties properties = new Properties();
        properties.setProperty(PROPERTY_USER, definition.username());
        properties.setProperty(PROPERTY_PASSWORD, definition.password());
        properties.setProperty(PROPERTY_CURRENT_SCHEMA, definition.schemaName());

        if (definition.sslMode() != null) {
            properties.setProperty(PROPERTY_SSL_MODE, toDriverValue(definition.sslMode()));
        }
        if (definition.connectTimeoutMs() != null) {
            properties.setProperty(PROPERTY_CONNECT_TIMEOUT, String.valueOf(toDriverTimeoutSeconds(definition.connectTimeoutMs())));
        }
        if (definition.socketTimeoutMs() != null) {
            properties.setProperty(PROPERTY_SOCKET_TIMEOUT, String.valueOf(toDriverTimeoutSeconds(definition.socketTimeoutMs())));
        }
        if (definition.applicationName() != null) {
            properties.setProperty(PROPERTY_APPLICATION_NAME, definition.applicationName());
        }
        if (requiresSslRootCert(definition.sslMode()) && definition.sslRootCertPath() != null) {
            properties.setProperty(PROPERTY_SSL_ROOT_CERT, definition.sslRootCertPath());
        }
        applyExtraOptions(properties, definition.extraJdbcOptionsJson());

        return properties;
    }

    private void applyExtraOptions(Properties properties, String extraJdbcOptionsJson) {
        if (extraJdbcOptionsJson == null || extraJdbcOptionsJson.isBlank()) {
            return;
        }

        Map<String, Object> options;
        try {
            options = objectMapper.readValue(extraJdbcOptionsJson, new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new InvalidExtraJdbcOptionsException("extraJdbcOptionsJson must be a valid flat JSON object");
        }

        if (options == null) {
            return;
        }

        for (Map.Entry<String, Object> entry : options.entrySet()) {
            String key = normalizeOptionKey(entry.getKey());
            ensureKeyIsAllowed(key);
            properties.setProperty(key, stringifyOptionValue(key, entry.getValue()));
        }
    }

    private String normalizeOptionKey(String key) {
        if (key == null || key.isBlank()) {
            throw new InvalidExtraJdbcOptionsException("extraJdbcOptionsJson contains an empty option key");
        }

        return key.trim();
    }

    private void ensureKeyIsAllowed(String key) {
        if (RESERVED_OPTION_KEYS.contains(key.toLowerCase(Locale.ROOT))) {
            throw new InvalidExtraJdbcOptionsException(
                    "extraJdbcOptionsJson cannot override reserved PostgreSQL connection property: " + key
            );
        }
    }

    private String stringifyOptionValue(String key, Object value) {
        if (value == null) {
            throw new InvalidExtraJdbcOptionsException(
                    "extraJdbcOptionsJson option '" + key + "' must not be null"
            );
        }
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }

        throw new InvalidExtraJdbcOptionsException(
                "extraJdbcOptionsJson option '" + key + "' must be a string, number, or boolean"
        );
    }

    private String toDriverValue(SslMode sslMode) {
        return sslMode.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private boolean requiresSslRootCert(SslMode sslMode) {
        return sslMode == SslMode.VERIFY_CA || sslMode == SslMode.VERIFY_FULL;
    }

    private int toDriverTimeoutSeconds(Integer timeoutMs) {
        return Math.max(1, (timeoutMs + 999) / 1000);
    }
}

