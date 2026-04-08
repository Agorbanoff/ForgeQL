package com.example.core.postgres.schema.validation;

import com.example.common.exceptions.InvalidSchemaSnapshotException;
import com.example.core.postgres.schema.model.GeneratedSchema;
import org.springframework.stereotype.Service;

@Service
public class SchemaValidationService {

    public void validate(GeneratedSchema schema) {
        if (schema == null) {
            throw new InvalidSchemaSnapshotException("Generated schema is required");
        }
        if (schema.datasourceId() == null) {
            throw new InvalidSchemaSnapshotException("Generated schema datasource id is required");
        }
    }
}

