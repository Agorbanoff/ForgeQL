package com.example.core.engine.schema;

import com.example.core.engine.introspection.PostgresIntrospectionResult;
import com.example.core.engine.schema.model.PostgresSchemaSnapshot;
import org.springframework.stereotype.Component;

@Component
public class SchemaAssembler {

    public PostgresSchemaSnapshot assemble(Integer datasourceId, PostgresIntrospectionResult introspectionResult) {
        throw new UnsupportedOperationException("Schema assembly is not implemented yet for datasource " + datasourceId);
    }
}
