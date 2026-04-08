package com.example.core.postgres.schema;

import com.example.core.postgres.introspection.PostgresIntrospectionResult;
import com.example.core.postgres.schema.model.PostgresSchemaSnapshot;
import org.springframework.stereotype.Component;

@Component
public class SchemaAssembler {

    public PostgresSchemaSnapshot assemble(Integer datasourceId, PostgresIntrospectionResult introspectionResult) {
        throw new UnsupportedOperationException("Schema assembly is not implemented yet for datasource " + datasourceId);
    }
}

