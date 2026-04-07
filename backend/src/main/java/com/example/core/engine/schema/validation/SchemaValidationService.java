package com.example.core.engine.schema.validation;

import com.example.common.CustomException;
import com.example.core.engine.schema.model.PostgresSchemaSnapshot;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class SchemaValidationService {

    public void validate(PostgresSchemaSnapshot snapshot) {
        if (snapshot == null) {
            throw new CustomException("Schema snapshot is required", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (snapshot.datasourceId() == null) {
            throw new CustomException("Schema snapshot datasource id is required", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
