package com.example.core.postgres.schema;

import com.example.core.postgres.introspection.PostgresIntrospectionResult;
import com.example.core.postgres.schema.model.GeneratedSchema;
import org.springframework.stereotype.Component;

@Component
public class SchemaAssembler {

    public GeneratedSchema assemble(Integer datasourceId, PostgresIntrospectionResult introspectionResult) {
        throw new UnsupportedOperationException("Schema assembly is not implemented yet for datasource " + datasourceId);
    }
}

