package com.example.core.postgres.schema;

import com.example.common.exceptions.GeneratedSchemaNotFoundException;
import com.example.core.postgres.schema.model.GeneratedSchema;
import com.example.core.postgres.schema.model.SchemaColumn;
import com.example.core.postgres.schema.model.SchemaRelation;
import com.example.core.postgres.schema.model.SchemaTable;
import com.example.core.postgres.schema.registry.SchemaRegistryService;
import com.example.service.DataSourceAuthorizationService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SchemaReadService {

    private final DataSourceAuthorizationService dataSourceAuthorizationService;
    private final SchemaRegistryService schemaRegistryService;
    private final PublicTableIdentifierResolver publicTableIdentifierResolver;

    public SchemaReadService(
            DataSourceAuthorizationService dataSourceAuthorizationService,
            SchemaRegistryService schemaRegistryService,
            PublicTableIdentifierResolver publicTableIdentifierResolver
    ) {
        this.dataSourceAuthorizationService = dataSourceAuthorizationService;
        this.schemaRegistryService = schemaRegistryService;
        this.publicTableIdentifierResolver = publicTableIdentifierResolver;
    }

    public GeneratedSchema getSchema(Integer datasourceId, Integer userId) {
        dataSourceAuthorizationService.getViewableDatasource(userId, datasourceId);
        return schemaRegistryService.findByDatasourceId(datasourceId)
                .orElseThrow(() -> new GeneratedSchemaNotFoundException(
                        "Schema is not generated for datasource " + datasourceId
                ));
    }

    public List<SchemaTable> getTables(Integer datasourceId, Integer userId) {
        return List.copyOf(getSchema(datasourceId, userId).tables().values());
    }

    public ResolvedTableIdentifier resolveTableIdentifier(Integer datasourceId, Integer userId, String tableIdentifier) {
        GeneratedSchema schema = getSchema(datasourceId, userId);
        return publicTableIdentifierResolver.resolve(schema, tableIdentifier);
    }

    public SchemaTable getTable(Integer datasourceId, Integer userId, String tableIdentifier) {
        return resolveTableIdentifier(datasourceId, userId, tableIdentifier).table();
    }

    public List<SchemaColumn> getTableColumns(Integer datasourceId, Integer userId, String tableIdentifier) {
        return resolveTableIdentifier(datasourceId, userId, tableIdentifier).table().columns();
    }

    public List<SchemaRelation> getTableRelations(Integer datasourceId, Integer userId, String tableIdentifier) {
        return resolveTableIdentifier(datasourceId, userId, tableIdentifier).table().relations();
    }
}
