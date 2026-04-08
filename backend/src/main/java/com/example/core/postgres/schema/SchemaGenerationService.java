package com.example.core.postgres.schema;

import com.example.common.exceptions.NoDataSourceFoundException;
import com.example.core.postgres.connection.PostgresRuntimeConnectionDefinition;
import com.example.core.postgres.connection.PostgresRuntimeConnectionResolver;
import com.example.core.postgres.introspection.PostgresIntrospectionResult;
import com.example.core.postgres.introspection.PostgresMetadataIntrospector;
import com.example.core.postgres.schema.model.GeneratedSchema;
import com.example.core.postgres.schema.registry.SchemaRegistryService;
import com.example.core.postgres.schema.validation.SchemaValidationService;
import com.example.persistence.model.DataSourceEntity;
import com.example.persistence.repository.DataSourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SchemaGenerationService {

    private final PostgresRuntimeConnectionResolver runtimeConnectionResolver;
    private final PostgresMetadataIntrospector metadataIntrospector;
    private final SchemaAssembler schemaAssembler;
    private final SchemaValidationService schemaValidationService;
    private final SchemaFingerprintService schemaFingerprintService;
    private final SchemaRegistryService schemaRegistryService;
    private final DataSourceRepository dataSourceRepository;

    public SchemaGenerationService(
            PostgresRuntimeConnectionResolver runtimeConnectionResolver,
            PostgresMetadataIntrospector metadataIntrospector,
            SchemaAssembler schemaAssembler,
            SchemaValidationService schemaValidationService,
            SchemaFingerprintService schemaFingerprintService,
            SchemaRegistryService schemaRegistryService,
            DataSourceRepository dataSourceRepository
    ) {
        this.runtimeConnectionResolver = runtimeConnectionResolver;
        this.metadataIntrospector = metadataIntrospector;
        this.schemaAssembler = schemaAssembler;
        this.schemaValidationService = schemaValidationService;
        this.schemaFingerprintService = schemaFingerprintService;
        this.schemaRegistryService = schemaRegistryService;
        this.dataSourceRepository = dataSourceRepository;
    }

    @Transactional
    public GeneratedSchema generate(Integer datasourceId, Integer userId) {
        DataSourceEntity dataSourceEntity = dataSourceRepository.findByIdAndUserAccount_Id(datasourceId, userId)
                .orElseThrow(() -> new NoDataSourceFoundException("Datasource not found"));

        PostgresRuntimeConnectionDefinition definition = runtimeConnectionResolver.resolve(dataSourceEntity);
        PostgresIntrospectionResult introspectionResult = metadataIntrospector.introspect(definition);
        GeneratedSchema assembledSchema = schemaAssembler.assemble(definition.datasourceId(), introspectionResult);

        schemaValidationService.validate(assembledSchema);

        GeneratedSchema generatedSchema = new GeneratedSchema(
                assembledSchema.datasourceId(),
                assembledSchema.serverVersion(),
                assembledSchema.generatedAt(),
                assembledSchema.defaultSchema(),
                schemaFingerprintService.fingerprint(assembledSchema),
                assembledSchema.tables(),
                assembledSchema.relationGraph()
        );

        schemaValidationService.validate(generatedSchema);
        schemaRegistryService.register(generatedSchema);

        dataSourceEntity.setLastSchemaGeneratedAt(generatedSchema.generatedAt());
        dataSourceEntity.setLastSchemaFingerprint(generatedSchema.fingerprint());
        dataSourceEntity.setServerVersion(generatedSchema.serverVersion());
        dataSourceRepository.save(dataSourceEntity);

        return generatedSchema;
    }
}

