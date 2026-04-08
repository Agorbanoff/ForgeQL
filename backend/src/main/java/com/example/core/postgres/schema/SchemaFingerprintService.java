package com.example.core.postgres.schema;

import com.example.core.postgres.schema.model.PostgresSchemaSnapshot;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class SchemaFingerprintService {

    private final SchemaSerializationService schemaSerializationService;

    public SchemaFingerprintService(SchemaSerializationService schemaSerializationService) {
        this.schemaSerializationService = schemaSerializationService;
    }

    public String fingerprint(PostgresSchemaSnapshot snapshot) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(schemaSerializationService.serialize(snapshot).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}

