export type DatabaseType = 'POSTGRESQL'

export type SslMode =
  | 'DISABLE'
  | 'PREFER'
  | 'REQUIRE'
  | 'VERIFY_CA'
  | 'VERIFY_FULL'

export type DatasourceStatus = 'ACTIVE' | 'INACTIVE'

export type DatasourceConnectionStatus =
  | 'UNTESTED'
  | 'SUCCEEDED'
  | 'FAILED'
  | 'TIMED_OUT'

export type SchemaTableType = 'TABLE' | 'VIEW' | 'MATERIALIZED_VIEW'

export type SchemaRelationType = 'ONE_TO_ONE' | 'ONE_TO_MANY' | 'MANY_TO_ONE'

export type SortDirection = 'ASC' | 'DESC'

export type AggregateFunction = 'count' | 'sum' | 'avg' | 'min' | 'max'

export type FilterOperator =
  | 'eq'
  | 'ne'
  | 'gt'
  | 'gte'
  | 'lt'
  | 'lte'
  | 'in'
  | 'between'
  | 'like'
  | 'ilike'
  | 'isNull'
  | 'isNotNull'

export type DatasourcePayload = {
  displayName: string
  dbType: DatabaseType
  host: string
  port: number
  databaseName: string
  schemaName: string
  username: string
  password: string
  sslMode: SslMode
  connectTimeoutMs?: number | null
  socketTimeoutMs?: number | null
  applicationName?: string | null
  sslRootCertRef?: string | null
  extraJdbcOptionsJson?: string | null
}

export type DatasourceRecord = {
  id: number
  ownerUserId: number
  displayName: string
  dbType: DatabaseType
  host: string
  port: number
  databaseName: string
  schemaName: string
  username: string
  sslMode: SslMode
  connectTimeoutMs: number | null
  socketTimeoutMs: number | null
  applicationName: string | null
  sslRootCertRef: string | null
  extraJdbcOptionsJson: string | null
  status: DatasourceStatus
  lastConnectionTestAt: string | null
  lastConnectionStatus: DatasourceConnectionStatus | null
  lastConnectionError: string | null
  lastSchemaGeneratedAt: string | null
  lastSchemaFingerprint: string | null
  serverVersion: string | null
  createdAt: string
  updatedAt: string
}

export type DatasourceConnectionTestResult = {
  datasourceId: number
  successful: boolean
  status: DatasourceStatus
  lastConnectionTestAt: string | null
  lastConnectionStatus: DatasourceConnectionStatus | null
  lastConnectionError: string | null
  databaseProductName: string | null
  serverVersion: string | null
  message: string
}

export type SchemaSummary = {
  datasourceId: number
  fingerprint: string
  generatedAt: string
  serverVersion: string | null
  tableCount: number
}

export type SchemaColumnCapabilities = {
  writable: boolean
  filterable: boolean
  sortable: boolean
  aggregatable: boolean
}

export type SchemaColumn = {
  name: string
  dbType: string
  javaType: string | null
  postgresTypeSchema: string | null
  postgresTypeName: string | null
  arrayElementTypeSchema: string | null
  arrayElementTypeName: string | null
  nullable: boolean
  identity: boolean
  generated: boolean
  defaultValue: string | null
  position: number
  capabilities: SchemaColumnCapabilities
  precision: number | null
  scale: number | null
  length: number | null
  enumType: boolean
  enumLabels: string[]
  uuidType: boolean
  jsonType: boolean
  jsonbType: boolean
  arrayType: boolean
  timestampWithoutTimeZone: boolean
  timestampWithTimeZone: boolean
  numericType: boolean
}

export type SchemaPrimaryKey = {
  columns: string[]
}

export type SchemaUniqueConstraint = {
  name: string
  columns: string[]
}

export type SchemaForeignKey = {
  name: string
  sourceSchema: string
  sourceTable: string
  sourceQualifiedName: string
  targetSchema: string
  targetTable: string
  targetQualifiedName: string
  sourceColumns: string[]
  targetColumns: string[]
}

export type SchemaRelation = {
  name: string
  relationType: SchemaRelationType
  sourceQualifiedName: string
  targetQualifiedName: string
  sourceColumns: string[]
  targetColumns: string[]
}

export type SchemaTableCapabilities = {
  read: boolean
  aggregate: boolean
  insert: boolean
  update: boolean
  delete: boolean
}

export type SchemaTable = {
  name: string
  schema: string
  qualifiedName: string
  tableType: SchemaTableType
  primaryKey: SchemaPrimaryKey | null
  uniqueConstraints: SchemaUniqueConstraint[]
  foreignKeys: SchemaForeignKey[]
  relations: SchemaRelation[]
  columns: SchemaColumn[]
  capabilities: SchemaTableCapabilities
}

export type GeneratedSchema = {
  datasourceId: number
  serverVersion: string | null
  generatedAt: string
  defaultSchema: string
  fingerprint: string
  tables: Record<string, SchemaTable>
  relationGraph: Record<string, string[]>
}

export type ReadFilter = {
  eq?: unknown
  ne?: unknown
  gt?: unknown
  gte?: unknown
  lt?: unknown
  lte?: unknown
  in?: unknown[]
  between?: unknown[]
  like?: string
  ilike?: string
  isNull?: boolean
  isNotNull?: boolean
}

export type ReadRowsRequest = {
  columns?: string[]
  filter?: Record<string, ReadFilter>
  sort?: Array<{
    field: string
    direction: SortDirection
  }>
  limit?: number
  offset?: number
}

export type RowsPage = {
  returnedCount: number
  limit: number | null
  offset: number | null
}

export type RowsResponse = {
  rows: Array<Record<string, unknown>>
  page: RowsPage
}

export type AggregateSelection = {
  function: AggregateFunction
  field?: string
  alias?: string
}

export type AggregateRequest = {
  selections: AggregateSelection[]
  groupBy?: string[]
  filter?: Record<string, ReadFilter>
}

export type AggregateResponse = {
  rows: Array<{
    values: Record<string, unknown>
  }>
}

export type CreateRowRequest = {
  values: Record<string, unknown>
}

export type UpdateRowRequest = {
  values: Record<string, unknown>
}

export type CreateRowResponse = {
  affectedRows: number
  createdIdentity: unknown
  row: Record<string, unknown> | null
}

export type UpdateRowResponse = {
  affectedRows: number
  row: Record<string, unknown> | null
}

export type DeleteRowResponse = {
  affectedRows: number
  deletedIdentity: unknown
}
