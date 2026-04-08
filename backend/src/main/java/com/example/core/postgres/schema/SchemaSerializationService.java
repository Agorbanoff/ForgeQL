package com.example.core.postgres.schema;

import com.example.common.exceptions.InvalidSchemaSnapshotException;
import com.example.common.exceptions.SchemaSerializationException;
import com.example.core.postgres.schema.model.GeneratedSchema;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class SchemaSerializationService {

    private final ObjectMapper objectMapper;

    public SchemaSerializationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serialize(GeneratedSchema snapshot) {
        if (snapshot == null) {
            throw new InvalidSchemaSnapshotException("Generated schema is required");
        }

        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new SchemaSerializationException("Failed to serialize generated schema", e);
        }
    }
}

