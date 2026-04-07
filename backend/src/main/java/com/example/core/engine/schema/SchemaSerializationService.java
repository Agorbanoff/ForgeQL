package com.example.core.engine.schema;

import com.example.core.engine.schema.model.PostgresSchemaSnapshot;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class SchemaSerializationService {

    private final ObjectMapper objectMapper;

    public SchemaSerializationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serialize(PostgresSchemaSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize schema snapshot", e);
        }
    }
}
