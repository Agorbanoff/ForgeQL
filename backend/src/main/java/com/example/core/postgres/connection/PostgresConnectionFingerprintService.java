package com.example.core.postgres.connection;

import com.example.common.exceptions.PostgresConnectionFingerprintException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Properties;
import java.util.TreeSet;

@Component
public class PostgresConnectionFingerprintService {

    public String fingerprint(PostgresJdbcConnectionConfig config) {
        if (config == null) {
            throw new PostgresConnectionFingerprintException("PostgreSQL JDBC connection config is required");
        }

        StringBuilder canonical = new StringBuilder();
        canonical.append(config.jdbcUrl() == null ? "" : config.jdbcUrl().trim()).append('\n');

        Properties properties = config.properties();
        TreeSet<String> sortedKeys = new TreeSet<>();
        if (properties != null) {
            sortedKeys.addAll(properties.stringPropertyNames());
        }

        for (String key : sortedKeys) {
            canonical.append(key)
                    .append('=')
                    .append(properties.getProperty(key, ""))
                    .append('\n');
        }

        return sha256(canonical.toString());
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new PostgresConnectionFingerprintException(
                    "Failed to compute PostgreSQL connection fingerprint",
                    e
            );
        }
    }
}
