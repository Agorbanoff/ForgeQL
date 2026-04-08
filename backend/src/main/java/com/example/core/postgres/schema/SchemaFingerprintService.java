package com.example.core.postgres.schema;

import com.example.common.exceptions.InvalidSchemaSnapshotException;
import com.example.common.exceptions.SchemaFingerprintComputationException;
import com.example.core.postgres.schema.model.GeneratedSchema;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SchemaFingerprintService {

    private final SchemaSerializationService schemaSerializationService;

    public SchemaFingerprintService(SchemaSerializationService schemaSerializationService) {
        this.schemaSerializationService = schemaSerializationService;
    }

    public String fingerprint(GeneratedSchema schema) {
        if (schema == null) {
            throw new InvalidSchemaSnapshotException("Generated schema is required");
        }

        GeneratedSchema fingerprintTarget = buildFingerprintTarget(schema);

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(
                    schemaSerializationService.serialize(fingerprintTarget).getBytes(StandardCharsets.UTF_8)
            );
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new SchemaFingerprintComputationException("Failed to compute schema fingerprint", e);
        }
    }

    private GeneratedSchema buildFingerprintTarget(GeneratedSchema schema) {
        LinkedHashMap<String, com.example.core.postgres.schema.model.SchemaTable> orderedTables = new LinkedHashMap<>();
        schema.tables().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> orderedTables.put(entry.getKey(), entry.getValue()));

        LinkedHashMap<String, List<String>> orderedRelationGraph = new LinkedHashMap<>();
        schema.relationGraph().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> orderedRelationGraph.put(
                        entry.getKey(),
                        entry.getValue().stream().sorted().toList()
                ));

        return new GeneratedSchema(
                null,
                null,
                null,
                null,
                null,
                Collections.unmodifiableMap(orderedTables),
                Collections.unmodifiableMap(orderedRelationGraph)
        );
    }
}

