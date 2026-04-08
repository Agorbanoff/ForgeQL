package com.example.core.postgres.schema;

import com.example.common.exceptions.AmbiguousTableIdentifierException;
import com.example.common.exceptions.GeneratedSchemaNotFoundException;
import com.example.common.exceptions.MissingRequiredFieldException;
import com.example.common.exceptions.NoDataSourceFoundException;
import com.example.common.exceptions.SchemaTableNotFoundException;
import com.example.core.postgres.schema.model.GeneratedSchema;
import com.example.core.postgres.schema.model.SchemaColumn;
import com.example.core.postgres.schema.model.SchemaRelation;
import com.example.core.postgres.schema.model.SchemaTable;
import com.example.core.postgres.schema.registry.SchemaRegistryService;
import com.example.persistence.repository.DataSourceRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SchemaReadService {

    private final DataSourceRepository dataSourceRepository;
    private final SchemaRegistryService schemaRegistryService;

    public SchemaReadService(
            DataSourceRepository dataSourceRepository,
            SchemaRegistryService schemaRegistryService
    ) {
        this.dataSourceRepository = dataSourceRepository;
        this.schemaRegistryService = schemaRegistryService;
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

    public SchemaTable getTable(Integer datasourceId, Integer userId, String tableIdentifier) {
        GeneratedSchema schema = getSchema(datasourceId, userId);
        return resolveTable(schema, tableIdentifier);
    }

    public List<SchemaColumn> getTableColumns(Integer datasourceId, Integer userId, String tableIdentifier) {
        return getTable(datasourceId, userId, tableIdentifier).columns();
    }

    public List<SchemaRelation> getTableRelations(Integer datasourceId, Integer userId, String tableIdentifier) {
        return getTable(datasourceId, userId, tableIdentifier).relations();
    }

    private void assertOwnedDatasource(Integer datasourceId, Integer userId) {
        dataSourceRepository.findByIdAndUserAccount_Id(datasourceId, userId)
                .orElseThrow(() -> new NoDataSourceFoundException("Datasource not found"));
    }

    private SchemaTable resolveTable(GeneratedSchema schema, String tableIdentifier) {
        if (tableIdentifier == null || tableIdentifier.isBlank()) {
            throw new MissingRequiredFieldException("tableName is required");
        }

        String normalizedIdentifier = tableIdentifier.trim();
        if (normalizedIdentifier.contains(".")) {
            SchemaTable qualifiedTable = schema.tables().get(normalizedIdentifier);
            if (qualifiedTable == null) {
                throw new SchemaTableNotFoundException("Table not found: " + normalizedIdentifier);
            }
            return qualifiedTable;
        }

        List<SchemaTable> matches = getSchemaTables(schema, normalizedIdentifier);

        return matches.get(0);
    }

    private static List<SchemaTable> getSchemaTables(GeneratedSchema schema, String normalizedIdentifier) {
        List<SchemaTable> matches = new ArrayList<>();
        for (SchemaTable table : schema.tables().values()) {
            if (table.name().equals(normalizedIdentifier)) {
                matches.add(table);
            }
        }

        if (matches.isEmpty()) {
            throw new SchemaTableNotFoundException("Table not found: " + normalizedIdentifier);
        }
        if (matches.size() > 1) {
            throw new AmbiguousTableIdentifierException(
                    "Table identifier '" + normalizedIdentifier
                            + "' is ambiguous. Use a schema-qualified identifier such as public."
                            + normalizedIdentifier
            );
        }
        return matches;
    }
}
