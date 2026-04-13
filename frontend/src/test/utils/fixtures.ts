import type {
  CurrentUser,
} from '../../api/accountApi'
import type {
  DatasourceConnectionStatus,
  DatasourceRecord,
  GeneratedSchema,
  RowsResponse,
  SchemaColumn,
  SchemaSummary,
  SchemaTable,
} from '../../types/platform'

export function buildCurrentUser(overrides: Partial<CurrentUser> = {}): CurrentUser {
  return {
    id: 1,
    email: 'user@example.com',
    ...overrides,
  }
}

export function buildDatasource(
  overrides: Partial<DatasourceRecord> = {}
): DatasourceRecord {
  return {
    id: 1,
    ownerUserId: 1,
    displayName: 'SUPABASEDB',
    dbType: 'POSTGRESQL',
    host: 'db.example.com',
    port: 5432,
    databaseName: 'postgres',
    schemaName: 'public',
    username: 'postgres',
    sslMode: 'PREFER',
    connectTimeoutMs: 5000,
    socketTimeoutMs: 5000,
    applicationName: 'ForgeQL',
    sslRootCertRef: null,
    extraJdbcOptionsJson: null,
    status: 'ACTIVE',
    lastConnectionTestAt: '2026-04-13T12:00:00.000Z',
    lastConnectionStatus: 'SUCCEEDED',
    lastConnectionError: null,
    lastSchemaGeneratedAt: '2026-04-13T12:01:00.000Z',
    lastSchemaFingerprint: 'schema-fingerprint',
    serverVersion: '16.3',
    createdAt: '2026-04-13T11:00:00.000Z',
    updatedAt: '2026-04-13T12:01:00.000Z',
    ...overrides,
  }
}

export function buildColumn(
  overrides: Partial<SchemaColumn> & Pick<SchemaColumn, 'name'>
): SchemaColumn {
  const { name, ...rest } = overrides

  return {
    name,
    dbType: 'text',
    javaType: 'String',
    postgresTypeSchema: 'pg_catalog',
    postgresTypeName: 'text',
    arrayElementTypeSchema: null,
    arrayElementTypeName: null,
    nullable: false,
    identity: false,
    generated: false,
    defaultValue: null,
    position: 1,
    capabilities: {
      writable: true,
      filterable: true,
      sortable: true,
      aggregatable: true,
    },
    precision: null,
    scale: null,
    length: null,
    enumType: false,
    enumLabels: [],
    uuidType: false,
    jsonType: false,
    jsonbType: false,
    arrayType: false,
    timestampWithoutTimeZone: false,
    timestampWithTimeZone: false,
    numericType: false,
    ...rest,
  }
}

export function buildTable(
  overrides: Partial<SchemaTable> & Pick<SchemaTable, 'name' | 'qualifiedName'>
): SchemaTable {
  const { name, qualifiedName, ...rest } = overrides

  return {
    name,
    schema: 'public',
    qualifiedName,
    tableType: 'TABLE',
    primaryKey: { columns: ['id'] },
    uniqueConstraints: [],
    foreignKeys: [],
    relations: [],
    columns: [
      buildColumn({
        name: 'id',
        dbType: 'int4',
        postgresTypeName: 'int4',
        javaType: 'Integer',
        position: 1,
        numericType: true,
        capabilities: {
          writable: false,
          filterable: true,
          sortable: true,
          aggregatable: true,
        },
      }),
      buildColumn({
        name: 'name',
        position: 2,
      }),
      buildColumn({
        name: 'amount',
        dbType: 'numeric',
        postgresTypeName: 'numeric',
        javaType: 'BigDecimal',
        position: 3,
        numericType: true,
      }),
    ],
    capabilities: {
      read: true,
      aggregate: true,
      insert: true,
      update: true,
      delete: true,
    },
    ...rest,
  }
}

export function buildSchemaSummary(
  overrides: Partial<SchemaSummary> = {}
): SchemaSummary {
  return {
    datasourceId: 1,
    fingerprint: 'schema-fingerprint',
    generatedAt: '2026-04-13T12:01:00.000Z',
    serverVersion: '16.3',
    tableCount: 1,
    ...overrides,
  }
}

export function buildGeneratedSchema(
  tables: SchemaTable[],
  datasourceId = 1
): GeneratedSchema {
  return {
    datasourceId,
    serverVersion: '16.3',
    generatedAt: '2026-04-13T12:01:00.000Z',
    defaultSchema: 'public',
    fingerprint: 'schema-fingerprint',
    tables: Object.fromEntries(
      tables.map((table) => [table.qualifiedName, table])
    ),
    relationGraph: {},
  }
}

export function buildRowsResponse(
  rows: Array<Record<string, unknown>>,
  limit = 25,
  offset = 0
): RowsResponse {
  return {
    rows,
    page: {
      returnedCount: rows.length,
      limit,
      offset,
    },
  }
}

export function withConnectionStatus(
  datasource: DatasourceRecord,
  status: DatasourceConnectionStatus | null
) {
  return {
    ...datasource,
    lastConnectionStatus: status,
  }
}
