package com.example.core.postgres.schema.validation;

import com.example.common.exceptions.InvalidSchemaSnapshotException;
import com.example.core.postgres.schema.model.GeneratedSchema;
import com.example.core.postgres.schema.model.SchemaColumn;
import com.example.core.postgres.schema.model.SchemaColumnCapabilities;
import com.example.core.postgres.schema.model.SchemaForeignKey;
import com.example.core.postgres.schema.model.SchemaPrimaryKey;
import com.example.core.postgres.schema.model.SchemaRelation;
import com.example.core.postgres.schema.model.SchemaTable;
import com.example.core.postgres.schema.model.SchemaTableCapabilities;
import com.example.core.postgres.schema.model.SchemaTableType;
import com.example.core.postgres.schema.model.SchemaUniqueConstraint;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class SchemaValidationService {

    public void validate(GeneratedSchema schema) {
        if (schema == null) {
            throw new InvalidSchemaSnapshotException("Generated schema is required");
        }
        if (schema.datasourceId() == null) {
            throw new InvalidSchemaSnapshotException("Generated schema datasource id is required");
        }
        requireText(schema.serverVersion(), "Generated schema server version is required");
        if (schema.generatedAt() == null) {
            throw new InvalidSchemaSnapshotException("Generated schema timestamp is required");
        }
        requireText(schema.defaultSchema(), "Generated schema default schema is required");
        if (schema.tables() == null) {
            throw new InvalidSchemaSnapshotException("Generated schema tables are required");
        }
        if (schema.relationGraph() == null) {
            throw new InvalidSchemaSnapshotException("Generated schema relation graph is required");
        }

        validateTables(schema);
        validateRelationGraph(schema);
    }

    private void validateTables(GeneratedSchema schema) {
        for (Map.Entry<String, SchemaTable> entry : schema.tables().entrySet()) {
            String mapKey = entry.getKey();
            SchemaTable table = entry.getValue();

            if (table == null) {
                throw new InvalidSchemaSnapshotException("Generated schema contains a null table entry");
            }

            requireText(mapKey, "Generated schema table key is required");
            requireText(table.name(), "Generated schema table name is required");
            requireText(table.schema(), "Generated schema table schema is required");
            requireText(table.qualifiedName(), "Generated schema table qualified name is required");
            if (!mapKey.equals(table.qualifiedName())) {
                throw new InvalidSchemaSnapshotException(
                        "Generated schema table key must match the table qualified name"
                );
            }
            if (table.tableType() == null) {
                throw new InvalidSchemaSnapshotException("Generated schema table type is required");
            }
            if (table.capabilities() == null) {
                throw new InvalidSchemaSnapshotException("Generated schema table capabilities are required");
            }
            if (table.columns() == null) {
                throw new InvalidSchemaSnapshotException("Generated schema table columns are required");
            }
            if (table.uniqueConstraints() == null) {
                throw new InvalidSchemaSnapshotException("Generated schema unique constraints are required");
            }
            if (table.foreignKeys() == null) {
                throw new InvalidSchemaSnapshotException("Generated schema foreign keys are required");
            }
            if (table.relations() == null) {
                throw new InvalidSchemaSnapshotException("Generated schema relations are required");
            }

            validateTableCapabilities(table);
            Set<String> columnNames = validateColumns(table);
            validatePrimaryKey(table.primaryKey(), table.qualifiedName(), columnNames);
            validateUniqueConstraints(table.uniqueConstraints(), table.qualifiedName(), columnNames);
            validateForeignKeys(schema, table.foreignKeys(), table, columnNames);
            validateRelations(schema, table.relations(), table, columnNames);
        }
    }

    private Set<String> validateColumns(SchemaTable table) {
        Set<String> columnNames = new LinkedHashSet<>();
        int previousPosition = 0;

        for (SchemaColumn column : table.columns()) {
            if (column == null) {
                throw new InvalidSchemaSnapshotException("Generated schema contains a null column entry");
            }

            requireText(column.name(), "Generated schema column name is required");
            requireText(column.dbType(), "Generated schema column dbType is required");
            requireText(column.javaType(), "Generated schema column javaType is required");
            if (column.position() <= 0) {
                throw new InvalidSchemaSnapshotException("Generated schema column position must be positive");
            }
            if (column.position() < previousPosition) {
                throw new InvalidSchemaSnapshotException("Generated schema columns must be ordered by position");
            }
            previousPosition = column.position();

            if (!columnNames.add(column.name())) {
                throw new InvalidSchemaSnapshotException(
                        "Generated schema contains duplicate column names for table " + table.qualifiedName()
                );
            }
            if (column.capabilities() == null) {
                throw new InvalidSchemaSnapshotException("Generated schema column capabilities are required");
            }
            if (column.enumLabels() == null) {
                throw new InvalidSchemaSnapshotException("Generated schema enum labels collection is required");
            }

            validateColumnCapabilities(table, column);
        }

        return columnNames;
    }

    private void validateTableCapabilities(SchemaTable table) {
        SchemaTableCapabilities capabilities = table.capabilities();
        boolean singleColumnPrimaryKey = table.primaryKey() != null && table.primaryKey().columns().size() == 1;

        switch (table.tableType()) {
            case TABLE -> {
                if (!capabilities.read() || !capabilities.aggregate()) {
                    throw new InvalidSchemaSnapshotException(
                            "Generated schema table capabilities must allow read and aggregate for TABLE objects"
                    );
                }
                if (singleColumnPrimaryKey) {
                    if (!capabilities.insert() || !capabilities.update() || !capabilities.delete()) {
                        throw new InvalidSchemaSnapshotException(
                                "Generated schema TABLE with a single primary key must be mutation-capable in v1"
                        );
                    }
                } else if (capabilities.insert() || capabilities.update() || capabilities.delete()) {
                    throw new InvalidSchemaSnapshotException(
                            "Generated schema TABLE without exactly one primary key must not be mutation-capable in v1"
                    );
                }
            }
            case VIEW, MATERIALIZED_VIEW -> {
                if (!capabilities.read() || !capabilities.aggregate()) {
                    throw new InvalidSchemaSnapshotException(
                            "Generated schema " + table.tableType() + " must allow read and aggregate"
                    );
                }
                if (capabilities.insert() || capabilities.update() || capabilities.delete()) {
                    throw new InvalidSchemaSnapshotException(
                            "Generated schema " + table.tableType() + " must not be mutation-capable in v1"
                    );
                }
            }
        }
    }

    private void validateColumnCapabilities(SchemaTable table, SchemaColumn column) {
        SchemaColumnCapabilities capabilities = column.capabilities();
        boolean expectedWritable = (table.capabilities().insert() || table.capabilities().update())
                && !isAutoGeneratedColumn(column);

        if (capabilities.writable() != expectedWritable) {
            throw new InvalidSchemaSnapshotException(
                    "Generated schema column writable capability is inconsistent for "
                            + table.qualifiedName() + "." + column.name()
            );
        }
    }

    private void validatePrimaryKey(
            SchemaPrimaryKey primaryKey,
            String tableQualifiedName,
            Set<String> columnNames
    ) {
        if (primaryKey == null) {
            return;
        }
        if (primaryKey.columns() == null || primaryKey.columns().isEmpty()) {
            throw new InvalidSchemaSnapshotException("Generated schema primary key columns are required");
        }
        validateExistingColumns(primaryKey.columns(), columnNames, tableQualifiedName, "primary key");
    }

    private void validateUniqueConstraints(
            List<SchemaUniqueConstraint> uniqueConstraints,
            String tableQualifiedName,
            Set<String> columnNames
    ) {
        for (SchemaUniqueConstraint uniqueConstraint : uniqueConstraints) {
            if (uniqueConstraint == null) {
                throw new InvalidSchemaSnapshotException("Generated schema contains a null unique constraint");
            }
            requireText(uniqueConstraint.name(), "Generated schema unique constraint name is required");
            if (uniqueConstraint.columns() == null || uniqueConstraint.columns().isEmpty()) {
                throw new InvalidSchemaSnapshotException("Generated schema unique constraint columns are required");
            }
            validateExistingColumns(
                    uniqueConstraint.columns(),
                    columnNames,
                    tableQualifiedName,
                    "unique constraint " + uniqueConstraint.name()
            );
        }
    }

    private void validateForeignKeys(
            GeneratedSchema schema,
            List<SchemaForeignKey> foreignKeys,
            SchemaTable table,
            Set<String> columnNames
    ) {
        for (SchemaForeignKey foreignKey : foreignKeys) {
            if (foreignKey == null) {
                throw new InvalidSchemaSnapshotException("Generated schema contains a null foreign key");
            }
            requireText(foreignKey.name(), "Generated schema foreign key name is required");
            requireText(foreignKey.sourceQualifiedName(), "Generated schema foreign key source is required");
            requireText(foreignKey.targetQualifiedName(), "Generated schema foreign key target is required");
            if (!table.qualifiedName().equals(foreignKey.sourceQualifiedName())) {
                throw new InvalidSchemaSnapshotException("Generated schema foreign key source table is inconsistent");
            }
            if (!table.schema().equals(foreignKey.sourceSchema()) || !table.name().equals(foreignKey.sourceTable())) {
                throw new InvalidSchemaSnapshotException("Generated schema foreign key source table metadata is inconsistent");
            }

            SchemaTable targetTable = schema.tables().get(foreignKey.targetQualifiedName());
            if (targetTable == null) {
                throw new InvalidSchemaSnapshotException("Generated schema foreign key target table must exist");
            }
            if (foreignKey.sourceColumns() == null || foreignKey.sourceColumns().isEmpty()) {
                throw new InvalidSchemaSnapshotException("Generated schema foreign key source columns are required");
            }
            if (foreignKey.targetColumns() == null || foreignKey.targetColumns().isEmpty()) {
                throw new InvalidSchemaSnapshotException("Generated schema foreign key target columns are required");
            }
            if (foreignKey.sourceColumns().size() != foreignKey.targetColumns().size()) {
                throw new InvalidSchemaSnapshotException("Generated schema foreign key column cardinality is inconsistent");
            }

            validateExistingColumns(
                    foreignKey.sourceColumns(),
                    columnNames,
                    table.qualifiedName(),
                    "foreign key " + foreignKey.name()
            );
            validateExistingColumns(
                    foreignKey.targetColumns(),
                    collectColumnNames(targetTable.columns()),
                    targetTable.qualifiedName(),
                    "foreign key " + foreignKey.name()
            );
        }
    }

    private void validateRelations(
            GeneratedSchema schema,
            List<SchemaRelation> relations,
            SchemaTable table,
            Set<String> columnNames
    ) {
        for (SchemaRelation relation : relations) {
            if (relation == null) {
                throw new InvalidSchemaSnapshotException("Generated schema contains a null relation");
            }
            requireText(relation.name(), "Generated schema relation name is required");
            requireText(relation.sourceQualifiedName(), "Generated schema relation source is required");
            requireText(relation.targetQualifiedName(), "Generated schema relation target is required");
            if (relation.relationType() == null) {
                throw new InvalidSchemaSnapshotException("Generated schema relation type is required");
            }
            if (!table.qualifiedName().equals(relation.sourceQualifiedName())) {
                throw new InvalidSchemaSnapshotException("Generated schema relation source table is inconsistent");
            }
            if (!schema.tables().containsKey(relation.targetQualifiedName())) {
                throw new InvalidSchemaSnapshotException("Generated schema relation target table must exist");
            }
            if (relation.sourceColumns() == null || relation.sourceColumns().isEmpty()) {
                throw new InvalidSchemaSnapshotException("Generated schema relation source columns are required");
            }
            if (relation.targetColumns() == null || relation.targetColumns().isEmpty()) {
                throw new InvalidSchemaSnapshotException("Generated schema relation target columns are required");
            }
            if (relation.sourceColumns().size() != relation.targetColumns().size()) {
                throw new InvalidSchemaSnapshotException("Generated schema relation column cardinality is inconsistent");
            }

            validateExistingColumns(
                    relation.sourceColumns(),
                    columnNames,
                    table.qualifiedName(),
                    "relation " + relation.name()
            );
            validateExistingColumns(
                    relation.targetColumns(),
                    collectColumnNames(schema.tables().get(relation.targetQualifiedName()).columns()),
                    relation.targetQualifiedName(),
                    "relation " + relation.name()
            );
        }
    }

    private void validateRelationGraph(GeneratedSchema schema) {
        for (Map.Entry<String, List<String>> entry : schema.relationGraph().entrySet()) {
            List<String> relatedTables = getStrings(schema, entry);

            for (String relatedTableQualifiedName : relatedTables) {
                requireText(relatedTableQualifiedName, "Generated schema relation graph target is required");
                if (!schema.tables().containsKey(relatedTableQualifiedName)) {
                    throw new InvalidSchemaSnapshotException("Generated schema relation graph target table must exist");
                }
            }
        }

        for (String tableQualifiedName : schema.tables().keySet()) {
            if (!schema.relationGraph().containsKey(tableQualifiedName)) {
                throw new InvalidSchemaSnapshotException("Generated schema relation graph must include every table");
            }
        }
    }

    private static List<String> getStrings(GeneratedSchema schema, Map.Entry<String, List<String>> entry) {
        String tableQualifiedName = entry.getKey();
        List<String> relatedTables = entry.getValue();

        if (!schema.tables().containsKey(tableQualifiedName)) {
            throw new InvalidSchemaSnapshotException("Generated schema relation graph contains an unknown table");
        }
        if (relatedTables == null) {
            throw new InvalidSchemaSnapshotException("Generated schema relation graph targets are required");
        }

        Set<String> distinctTargets = new LinkedHashSet<>(relatedTables);
        if (distinctTargets.size() != relatedTables.size()) {
            throw new InvalidSchemaSnapshotException("Generated schema relation graph contains duplicate targets");
        }
        return relatedTables;
    }

    private Set<String> collectColumnNames(List<SchemaColumn> columns) {
        Set<String> columnNames = new LinkedHashSet<>();
        for (SchemaColumn column : columns) {
            columnNames.add(column.name());
        }
        return columnNames;
    }

    private void validateExistingColumns(
            List<String> referencedColumns,
            Set<String> availableColumns,
            String tableQualifiedName,
            String owner
    ) {
        for (String referencedColumn : referencedColumns) {
            requireText(referencedColumn, "Generated schema referenced column is required");
            if (!availableColumns.contains(referencedColumn)) {
                throw new InvalidSchemaSnapshotException(
                        "Generated schema " + owner + " references unknown column "
                                + tableQualifiedName + "." + referencedColumn
                );
            }
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new InvalidSchemaSnapshotException(message);
        }
    }

    private boolean isAutoGeneratedColumn(SchemaColumn column) {
        return column.identity() || column.generated() || hasSequenceGeneratedDefault(column.defaultValue());
    }

    private boolean hasSequenceGeneratedDefault(String defaultValue) {
        if (defaultValue == null || defaultValue.isBlank()) {
            return false;
        }
        return defaultValue.trim().toLowerCase(Locale.ROOT).startsWith("nextval(");
    }
}