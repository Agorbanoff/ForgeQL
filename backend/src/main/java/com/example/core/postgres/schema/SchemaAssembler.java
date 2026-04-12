package com.example.core.postgres.schema;

import com.example.common.exceptions.InvalidSchemaSnapshotException;
import com.example.core.postgres.introspection.ColumnMetadata;
import com.example.core.postgres.introspection.EnumMetadata;
import com.example.core.postgres.introspection.ForeignKeyMetadata;
import com.example.core.postgres.introspection.PostgresIntrospectionResult;
import com.example.core.postgres.introspection.PostgresRawTableType;
import com.example.core.postgres.introspection.PrimaryKeyMetadata;
import com.example.core.postgres.introspection.TableMetadata;
import com.example.core.postgres.introspection.UniqueConstraintMetadata;
import com.example.core.postgres.schema.model.GeneratedSchema;
import com.example.core.postgres.schema.model.SchemaColumn;
import com.example.core.postgres.schema.model.SchemaColumnCapabilities;
import com.example.core.postgres.schema.model.SchemaForeignKey;
import com.example.core.postgres.schema.model.SchemaPrimaryKey;
import com.example.core.postgres.schema.model.SchemaRelation;
import com.example.core.postgres.schema.model.SchemaRelationType;
import com.example.core.postgres.schema.model.SchemaTable;
import com.example.core.postgres.schema.model.SchemaTableCapabilities;
import com.example.core.postgres.schema.model.SchemaTableType;
import com.example.core.postgres.schema.model.SchemaUniqueConstraint;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class SchemaAssembler {

    public GeneratedSchema assemble(Integer datasourceId, PostgresIntrospectionResult introspectionResult) {
        if (datasourceId == null) {
            throw new InvalidSchemaSnapshotException("Datasource id is required for schema assembly");
        }
        if (introspectionResult == null) {
            throw new InvalidSchemaSnapshotException("PostgreSQL introspection result is required");
        }

        Map<EnumTypeKey, List<String>> enumLabelsByType = buildEnumLabelsByType(introspectionResult.enums());
        Map<String, List<ColumnMetadata>> columnsByTable = groupColumnsByTable(introspectionResult.columns());
        Map<String, PrimaryKeyMetadata> primaryKeysByTable = mapPrimaryKeysByTable(introspectionResult.primaryKeys());
        Map<String, List<UniqueConstraintMetadata>> uniqueConstraintsByTable =
                groupUniqueConstraintsByTable(introspectionResult.uniqueConstraints());
        Map<String, List<ForeignKeyMetadata>> foreignKeysBySourceTable =
                groupForeignKeysBySourceTable(introspectionResult.foreignKeys());
        Map<String, List<ForeignKeyMetadata>> foreignKeysByTargetTable =
                groupForeignKeysByTargetTable(introspectionResult.foreignKeys());

        List<TableMetadata> sortedTables = new ArrayList<>(introspectionResult.tables());
        sortedTables.sort(
                Comparator.comparing(TableMetadata::schemaName)
                        .thenComparing(TableMetadata::tableName)
        );

        LinkedHashMap<String, SchemaTable> tablesByQualifiedName = new LinkedHashMap<>();
        for (TableMetadata tableMetadata : sortedTables) {
            String qualifiedName = qualifiedName(tableMetadata.schemaName(), tableMetadata.tableName());
            SchemaPrimaryKey primaryKey = buildPrimaryKey(primaryKeysByTable.get(qualifiedName));
            List<SchemaUniqueConstraint> uniqueConstraints = buildUniqueConstraints(
                    uniqueConstraintsByTable.getOrDefault(qualifiedName, List.of())
            );
            SchemaTableCapabilities tableCapabilities = buildTableCapabilities(tableMetadata.tableType(), primaryKey);
            List<SchemaColumn> columns = buildColumns(
                    columnsByTable.getOrDefault(qualifiedName, List.of()),
                    enumLabelsByType,
                    tableCapabilities
            );
            List<SchemaForeignKey> foreignKeys = buildForeignKeys(
                    tableMetadata,
                    foreignKeysBySourceTable.getOrDefault(qualifiedName, List.of())
            );

            tablesByQualifiedName.put(
                    qualifiedName,
                    new SchemaTable(
                            tableMetadata.tableName(),
                            tableMetadata.schemaName(),
                            qualifiedName,
                            mapTableType(tableMetadata.tableType()),
                            primaryKey,
                            uniqueConstraints,
                            foreignKeys,
                            List.of(),
                            columns,
                            tableCapabilities
                    )
            );
        }

        LinkedHashMap<String, SchemaTable> tablesWithRelations = addRelations(
                tablesByQualifiedName,
                foreignKeysBySourceTable,
                foreignKeysByTargetTable
        );

        return new GeneratedSchema(
                datasourceId,
                introspectionResult.serverVersion(),
                Instant.now(),
                introspectionResult.defaultSchema(),
                null,
                Collections.unmodifiableMap(tablesWithRelations),
                Collections.unmodifiableMap(buildRelationGraph(tablesWithRelations))
        );
    }

    private Map<EnumTypeKey, List<String>> buildEnumLabelsByType(List<EnumMetadata> enums) {
        LinkedHashMap<EnumTypeKey, List<String>> enumLabelsByType = new LinkedHashMap<>();
        for (EnumMetadata enumMetadata : enums) {
            enumLabelsByType.put(
                    new EnumTypeKey(enumMetadata.schemaName(), enumMetadata.enumName()),
                    List.copyOf(enumMetadata.labels())
            );
        }
        return enumLabelsByType;
    }

    private Map<String, List<ColumnMetadata>> groupColumnsByTable(List<ColumnMetadata> columns) {
        LinkedHashMap<String, List<ColumnMetadata>> columnsByTable = new LinkedHashMap<>();
        for (ColumnMetadata column : columns) {
            columnsByTable.computeIfAbsent(
                    qualifiedName(column.schemaName(), column.tableName()),
                    ignored -> new ArrayList<>()
            ).add(column);
        }
        columnsByTable.replaceAll((ignored, value) -> {
            List<ColumnMetadata> sortedColumns = new ArrayList<>(value);
            sortedColumns.sort(
                    Comparator.comparingInt(ColumnMetadata::ordinalPosition)
                            .thenComparing(ColumnMetadata::columnName)
            );
            return List.copyOf(sortedColumns);
        });
        return columnsByTable;
    }

    private Map<String, PrimaryKeyMetadata> mapPrimaryKeysByTable(List<PrimaryKeyMetadata> primaryKeys) {
        LinkedHashMap<String, PrimaryKeyMetadata> primaryKeysByTable = new LinkedHashMap<>();
        for (PrimaryKeyMetadata primaryKey : primaryKeys) {
            primaryKeysByTable.put(qualifiedName(primaryKey.schemaName(), primaryKey.tableName()), primaryKey);
        }
        return primaryKeysByTable;
    }

    private Map<String, List<UniqueConstraintMetadata>> groupUniqueConstraintsByTable(
            List<UniqueConstraintMetadata> uniqueConstraints
    ) {
        LinkedHashMap<String, List<UniqueConstraintMetadata>> constraintsByTable = new LinkedHashMap<>();
        for (UniqueConstraintMetadata uniqueConstraint : uniqueConstraints) {
            constraintsByTable.computeIfAbsent(
                    qualifiedName(uniqueConstraint.schemaName(), uniqueConstraint.tableName()),
                    ignored -> new ArrayList<>()
            ).add(uniqueConstraint);
        }
        constraintsByTable.replaceAll((ignored, value) -> {
            List<UniqueConstraintMetadata> sortedConstraints = new ArrayList<>(value);
            sortedConstraints.sort(Comparator.comparing(UniqueConstraintMetadata::constraintName));
            return List.copyOf(sortedConstraints);
        });
        return constraintsByTable;
    }

    private Map<String, List<ForeignKeyMetadata>> groupForeignKeysBySourceTable(List<ForeignKeyMetadata> foreignKeys) {
        LinkedHashMap<String, List<ForeignKeyMetadata>> foreignKeysByTable = new LinkedHashMap<>();
        for (ForeignKeyMetadata foreignKey : foreignKeys) {
            foreignKeysByTable.computeIfAbsent(
                    qualifiedName(foreignKey.sourceSchemaName(), foreignKey.sourceTableName()),
                    ignored -> new ArrayList<>()
            ).add(foreignKey);
        }
        foreignKeysByTable.replaceAll((ignored, value) -> sortForeignKeys(value));
        return foreignKeysByTable;
    }

    private Map<String, List<ForeignKeyMetadata>> groupForeignKeysByTargetTable(List<ForeignKeyMetadata> foreignKeys) {
        LinkedHashMap<String, List<ForeignKeyMetadata>> foreignKeysByTable = new LinkedHashMap<>();
        for (ForeignKeyMetadata foreignKey : foreignKeys) {
            foreignKeysByTable.computeIfAbsent(
                    qualifiedName(foreignKey.targetSchemaName(), foreignKey.targetTableName()),
                    ignored -> new ArrayList<>()
            ).add(foreignKey);
        }
        foreignKeysByTable.replaceAll((ignored, value) -> sortForeignKeys(value));
        return foreignKeysByTable;
    }

    private List<ForeignKeyMetadata> sortForeignKeys(List<ForeignKeyMetadata> foreignKeys) {
        List<ForeignKeyMetadata> sortedForeignKeys = new ArrayList<>(foreignKeys);
        sortedForeignKeys.sort(
                Comparator.comparing(ForeignKeyMetadata::constraintName)
                        .thenComparing(ForeignKeyMetadata::sourceSchemaName)
                        .thenComparing(ForeignKeyMetadata::sourceTableName)
                        .thenComparing(ForeignKeyMetadata::targetSchemaName)
                        .thenComparing(ForeignKeyMetadata::targetTableName)
        );
        return List.copyOf(sortedForeignKeys);
    }

    private SchemaPrimaryKey buildPrimaryKey(PrimaryKeyMetadata primaryKeyMetadata) {
        if (primaryKeyMetadata == null) {
            return null;
        }
        return new SchemaPrimaryKey(List.copyOf(primaryKeyMetadata.columns()));
    }

    private List<SchemaUniqueConstraint> buildUniqueConstraints(List<UniqueConstraintMetadata> uniqueConstraints) {
        List<SchemaUniqueConstraint> result = new ArrayList<>();
        for (UniqueConstraintMetadata uniqueConstraint : uniqueConstraints) {
            result.add(
                    new SchemaUniqueConstraint(
                            uniqueConstraint.constraintName(),
                            List.copyOf(uniqueConstraint.columns())
                    )
            );
        }
        return List.copyOf(result);
    }

    private List<SchemaColumn> buildColumns(
            List<ColumnMetadata> columns,
            Map<EnumTypeKey, List<String>> enumLabelsByType,
            SchemaTableCapabilities tableCapabilities
    ) {
        List<SchemaColumn> result = new ArrayList<>();
        for (ColumnMetadata column : columns) {
            boolean arrayType = column.elementTypeName() != null && !column.elementTypeName().isBlank();
            TypeReference effectiveType = resolveEffectiveType(column, arrayType);
            List<String> enumLabels = enumLabelsByType.getOrDefault(
                    new EnumTypeKey(effectiveType.schemaName(), effectiveType.typeName()),
                    List.of()
            );
            boolean enumType = !enumLabels.isEmpty();
            boolean uuidType = isType(column, "uuid");
            boolean jsonType = isType(column, "json");
            boolean jsonbType = isType(column, "jsonb");
            boolean timestampWithoutTimeZone = isType(column, "timestamp");
            boolean timestampWithTimeZone = isType(column, "timestamptz");
            boolean numericType = isType(column, "numeric");

            result.add(
                    new SchemaColumn(
                            column.columnName(),
                            column.dbType(),
                            mapJavaType(column, arrayType, enumType, uuidType, jsonType, jsonbType,
                                    timestampWithoutTimeZone, timestampWithTimeZone, numericType),
                            column.typeSchemaName(),
                            column.typeName(),
                            column.elementTypeSchemaName(),
                            column.elementTypeName(),
                            column.nullable(),
                            column.identity(),
                            column.generated(),
                            column.defaultValue(),
                            column.ordinalPosition(),
                            buildColumnCapabilities(column, tableCapabilities, arrayType, jsonType, jsonbType),
                            column.precision(),
                            column.scale(),
                            column.length(),
                            enumType,
                            List.copyOf(enumLabels),
                            uuidType,
                            jsonType,
                            jsonbType,
                            arrayType,
                            timestampWithoutTimeZone,
                            timestampWithTimeZone,
                            numericType
                    )
            );
        }
        return List.copyOf(result);
    }

    private List<SchemaForeignKey> buildForeignKeys(TableMetadata tableMetadata, List<ForeignKeyMetadata> foreignKeys) {
        List<SchemaForeignKey> result = new ArrayList<>();
        String sourceQualifiedName = qualifiedName(tableMetadata.schemaName(), tableMetadata.tableName());
        for (ForeignKeyMetadata foreignKey : foreignKeys) {
            result.add(
                    new SchemaForeignKey(
                            foreignKey.constraintName(),
                            tableMetadata.schemaName(),
                            tableMetadata.tableName(),
                            sourceQualifiedName,
                            foreignKey.targetSchemaName(),
                            foreignKey.targetTableName(),
                            qualifiedName(foreignKey.targetSchemaName(), foreignKey.targetTableName()),
                            List.copyOf(foreignKey.sourceColumns()),
                            List.copyOf(foreignKey.targetColumns())
                    )
            );
        }
        return List.copyOf(result);
    }

    private LinkedHashMap<String, SchemaTable> addRelations(
            Map<String, SchemaTable> tablesByQualifiedName,
            Map<String, List<ForeignKeyMetadata>> foreignKeysBySourceTable,
            Map<String, List<ForeignKeyMetadata>> foreignKeysByTargetTable
    ) {
        LinkedHashMap<String, SchemaTable> tablesWithRelations = new LinkedHashMap<>();

        for (SchemaTable table : tablesByQualifiedName.values()) {
            List<SchemaRelation> relations = buildRelationsForTable(
                    table,
                    tablesByQualifiedName,
                    foreignKeysBySourceTable.getOrDefault(table.qualifiedName(), List.of()),
                    foreignKeysByTargetTable.getOrDefault(table.qualifiedName(), List.of())
            );

            tablesWithRelations.put(
                    table.qualifiedName(),
                    new SchemaTable(
                            table.name(),
                            table.schema(),
                            table.qualifiedName(),
                            table.tableType(),
                            table.primaryKey(),
                            table.uniqueConstraints(),
                            table.foreignKeys(),
                            relations,
                            table.columns(),
                            table.capabilities()
                    )
            );
        }

        return tablesWithRelations;
    }

    private List<SchemaRelation> buildRelationsForTable(
            SchemaTable table,
            Map<String, SchemaTable> tablesByQualifiedName,
            List<ForeignKeyMetadata> outgoingForeignKeys,
            List<ForeignKeyMetadata> incomingForeignKeys
    ) {
        List<SchemaRelation> relations = new ArrayList<>();

        for (ForeignKeyMetadata foreignKey : outgoingForeignKeys) {
            SchemaRelationType forwardRelationType = determineForwardRelationType(table, foreignKey.sourceColumns());
            relations.add(
                    new SchemaRelation(
                            foreignKey.constraintName(),
                            forwardRelationType,
                            table.qualifiedName(),
                            qualifiedName(foreignKey.targetSchemaName(), foreignKey.targetTableName()),
                            List.copyOf(foreignKey.sourceColumns()),
                            List.copyOf(foreignKey.targetColumns())
                    )
            );
        }

        for (ForeignKeyMetadata foreignKey : incomingForeignKeys) {
            SchemaTable sourceTable = tablesByQualifiedName.get(
                    qualifiedName(foreignKey.sourceSchemaName(), foreignKey.sourceTableName())
            );
            SchemaRelationType forwardRelationType = determineForwardRelationType(sourceTable, foreignKey.sourceColumns());
            relations.add(
                    new SchemaRelation(
                            foreignKey.constraintName(),
                            forwardRelationType == SchemaRelationType.ONE_TO_ONE
                                    ? SchemaRelationType.ONE_TO_ONE
                                    : SchemaRelationType.ONE_TO_MANY,
                            table.qualifiedName(),
                            qualifiedName(foreignKey.sourceSchemaName(), foreignKey.sourceTableName()),
                            List.copyOf(foreignKey.targetColumns()),
                            List.copyOf(foreignKey.sourceColumns())
                    )
            );
        }

        relations.sort(
                Comparator.comparing(SchemaRelation::targetQualifiedName)
                        .thenComparing(SchemaRelation::name)
                        .thenComparing(relation -> relation.relationType().name())
        );
        return List.copyOf(relations);
    }

    private LinkedHashMap<String, List<String>> buildRelationGraph(Map<String, SchemaTable> tablesByQualifiedName) {
        LinkedHashMap<String, List<String>> relationGraph = new LinkedHashMap<>();
        for (SchemaTable table : tablesByQualifiedName.values()) {
            Set<String> relatedTables = new LinkedHashSet<>();
            for (SchemaRelation relation : table.relations()) {
                relatedTables.add(relation.targetQualifiedName());
            }
            relationGraph.put(table.qualifiedName(), List.copyOf(relatedTables));
        }
        return relationGraph;
    }

    private SchemaTableCapabilities buildTableCapabilities(PostgresRawTableType tableType, SchemaPrimaryKey primaryKey) {
        return switch (tableType) {
            case TABLE -> {
                boolean singleColumnPrimaryKey = primaryKey != null && primaryKey.columns().size() == 1;
                yield new SchemaTableCapabilities(
                        true,
                        true,
                        singleColumnPrimaryKey,
                        singleColumnPrimaryKey,
                        singleColumnPrimaryKey
                );
            }
            case VIEW, MATERIALIZED_VIEW -> new SchemaTableCapabilities(true, true, false, false, false);
        };
    }

    private SchemaColumnCapabilities buildColumnCapabilities(
            ColumnMetadata column,
            SchemaTableCapabilities tableCapabilities,
            boolean arrayType,
            boolean jsonType,
            boolean jsonbType
    ) {
        boolean writable = (tableCapabilities.insert() || tableCapabilities.update())
                && !isAutoGeneratedColumn(column);
        boolean filterable = !arrayType && !jsonType && !jsonbType;
        boolean sortable = !arrayType && !jsonType && !jsonbType;
        boolean aggregatable = !arrayType && !jsonType && !jsonbType;

        return new SchemaColumnCapabilities(
                writable,
                filterable,
                sortable,
                aggregatable
        );
    }

    private boolean isAutoGeneratedColumn(ColumnMetadata column) {
        return column.identity() || column.generated() || hasSequenceGeneratedDefault(column.defaultValue());
    }

    private boolean hasSequenceGeneratedDefault(String defaultValue) {
        if (defaultValue == null || defaultValue.isBlank()) {
            return false;
        }
        return defaultValue.trim().toLowerCase(Locale.ROOT).startsWith("nextval(");
    }

    private SchemaRelationType determineForwardRelationType(SchemaTable sourceTable, List<String> sourceColumns) {
        if (sourceTable == null) {
            throw new InvalidSchemaSnapshotException("Source table is required to derive schema relations");
        }
        if (matchesPrimaryKey(sourceTable.primaryKey(), sourceColumns) || matchesUniqueConstraint(sourceTable, sourceColumns)) {
            return SchemaRelationType.ONE_TO_ONE;
        }
        return SchemaRelationType.MANY_TO_ONE;
    }

    private boolean matchesPrimaryKey(SchemaPrimaryKey primaryKey, List<String> sourceColumns) {
        return primaryKey != null && primaryKey.columns().equals(sourceColumns);
    }

    private boolean matchesUniqueConstraint(SchemaTable sourceTable, List<String> sourceColumns) {
        for (SchemaUniqueConstraint uniqueConstraint : sourceTable.uniqueConstraints()) {
            if (uniqueConstraint.columns().equals(sourceColumns)) {
                return true;
            }
        }
        return false;
    }

    private SchemaTableType mapTableType(PostgresRawTableType tableType) {
        return switch (tableType) {
            case TABLE -> SchemaTableType.TABLE;
            case VIEW -> SchemaTableType.VIEW;
            case MATERIALIZED_VIEW -> SchemaTableType.MATERIALIZED_VIEW;
        };
    }

    private TypeReference resolveEffectiveType(ColumnMetadata column, boolean arrayType) {
        if (arrayType) {
            return new TypeReference(column.elementTypeSchemaName(), column.elementTypeName());
        }
        return new TypeReference(column.typeSchemaName(), column.typeName());
    }

    private boolean isType(ColumnMetadata column, String typeName) {
        return typeName.equals(column.typeName()) || typeName.equals(column.elementTypeName());
    }

    private String mapJavaType(
            ColumnMetadata column,
            boolean arrayType,
            boolean enumType,
            boolean uuidType,
            boolean jsonType,
            boolean jsonbType,
            boolean timestampWithoutTimeZone,
            boolean timestampWithTimeZone,
            boolean numericType
    ) {
        if (arrayType) {
            return "List";
        }
        if (enumType) {
            return "String";
        }
        if (uuidType) {
            return "UUID";
        }
        if (jsonType || jsonbType) {
            return "Object";
        }
        if (timestampWithTimeZone) {
            return "OffsetDateTime";
        }
        if (timestampWithoutTimeZone) {
            return "LocalDateTime";
        }
        if (numericType) {
            return "BigDecimal";
        }

        String normalizedType = column.typeName() == null ? "" : column.typeName().toLowerCase(Locale.ROOT);
        return switch (normalizedType) {
            case "bool" -> "Boolean";
            case "int2" -> "Short";
            case "int4" -> "Integer";
            case "int8" -> "Long";
            case "float4" -> "Float";
            case "float8" -> "Double";
            case "date" -> "LocalDate";
            case "time" -> "LocalTime";
            case "timetz" -> "OffsetTime";
            case "bytea" -> "byte[]";
            default -> "String";
        };
    }

    private String qualifiedName(String schemaName, String tableName) {
        return schemaName + "." + tableName;
    }

    private record EnumTypeKey(String schemaName, String enumName) {
    }

    private record TypeReference(String schemaName, String typeName) {
    }
}
