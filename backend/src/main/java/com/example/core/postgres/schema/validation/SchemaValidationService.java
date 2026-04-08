package com.example.core.postgres.schema.validation;

import com.example.common.exceptions.InvalidSchemaSnapshotException;
import com.example.core.postgres.schema.model.PostgresSchemaSnapshot;
import org.springframework.stereotype.Service;

@Service
public class SchemaValidationService {

    public void validate(PostgresSchemaSnapshot snapshot) {
        if (snapshot == null) {
            throw new InvalidSchemaSnapshotException("Schema snapshot is required");
        }
        if (snapshot.datasourceId() == null) {
            throw new InvalidSchemaSnapshotException("Schema snapshot datasource id is required");
        }
    }
}

