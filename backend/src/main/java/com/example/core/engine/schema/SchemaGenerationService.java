package com.example.core.engine.schema;

import com.example.core.engine.connection.PostgresRuntimeConnectionDefinition;
import com.example.core.engine.connection.PostgresRuntimeConnectionResolver;
import com.example.core.engine.introspection.PostgresMetadataIntrospector;
import com.example.core.engine.schema.model.PostgresSchemaSnapshot;
import com.example.core.engine.schema.registry.SchemaRegistryService;
import com.example.core.engine.schema.validation.SchemaValidationService;
import org.springframework.stereotype.Service;

@Service
public class SchemaGenerationService {

    private final PostgresRuntimeConnectionResolver runtimeConnectionResolver;
    private final PostgresMetadataIntrospector metadataIntrospector;
    private final SchemaAssembler schemaAssembler;
    private final SchemaValidationService schemaValidationService;
    private final SchemaFingerprintService schemaFingerprintService;
    private final SchemaRegistryService schemaRegistryService;

    public SchemaGenerationService(
            PostgresRuntimeConnectionResolver runtimeConnectionResolver,
            PostgresMetadataIntrospector metadataIntrospector,
            SchemaAssembler schemaAssembler,
            SchemaValidationService schemaValidationService,
            SchemaFingerprintService schemaFingerprintService,
            SchemaRegistryService schemaRegistryService
    ) {
        this.runtimeConnectionResolver = runtimeConnectionResolver;
        this.metadataIntrospector = metadataIntrospector;
        this.schemaAssembler = schemaAssembler;
        this.schemaValidationService = schemaValidationService;
        this.schemaFingerprintService = schemaFingerprintService;
        this.schemaRegistryService = schemaRegistryService;
    }

    public PostgresSchemaSnapshot generate(Integer datasourceId, Integer userId) {
        PostgresRuntimeConnectionDefinition definition = runtimeConnectionResolver.resolve(datasourceId, userId);
        throw new UnsupportedOperationException("Schema generation is not implemented yet for datasource " + definition.datasourceId());
    }
}
