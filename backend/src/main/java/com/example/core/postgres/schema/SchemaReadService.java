package com.example.core.postgres.schema;

import com.example.common.exceptions.GeneratedSchemaNotFoundException;
import com.example.common.exceptions.NoDataSourceFoundException;
import com.example.core.postgres.schema.model.GeneratedSchema;
import com.example.core.postgres.schema.model.SchemaColumn;
import com.example.core.postgres.schema.model.SchemaRelation;
import com.example.core.postgres.schema.model.SchemaTable;
import com.example.core.postgres.schema.registry.SchemaRegistryService;
import com.example.persistence.repository.DataSourceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SchemaReadService {

    private final DataSourceRepository dataSourceRepository;
    private final SchemaRegistryService schemaRegistryService;
    private final PublicTableIdentifierResolver publicTableIdentifierResolver;

    public SchemaReadService(
            DataSourceRepository dataSourceRepository,
            SchemaRegistryService schemaRegistryService,
            PublicTableIdentifierResolver publicTableIdentifierResolver
    ) {
        this.dataSourceRepository = dataSourceRepository;
        this.schemaRegistryService = schemaRegistryService;
        this.publicTableIdentifierResolver = publicTableIdentifierResolver;
    }

    public GeneratedSchema getSchema(Integer datasourceId, Integer userId) {
        assertOwnedDatasource(datasourceId, userId);
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

    private void assertOwnedDatasource(Integer datasourceId, Integer userId) {
        dataSourceRepository.findByIdAndUserAccount_Id(datasourceId, userId)
                .orElseThrow(() -> new NoDataSourceFoundException("Datasource not found"));
    }
}
