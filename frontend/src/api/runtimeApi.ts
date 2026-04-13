import { apiFetch, buildApiRequestError, parseResponseBody } from './http'
import type {
  AggregateRequest,
  AggregateResponse,
  CreateRowRequest,
  CreateRowResponse,
  DeleteRowResponse,
  GeneratedSchema,
  ReadRowsRequest,
  RowsResponse,
  SchemaTable,
  SchemaSummary,
  UpdateRowRequest,
  UpdateRowResponse,
} from '../types/platform'

type SchemaEnvelope = {
  schema: GeneratedSchema
}

type TablesEnvelope = {
  tables: SchemaTable[]
}

type TableEnvelope = {
  table: SchemaTable
}

function encodeTableName(tableName: string) {
  return encodeURIComponent(tableName)
}

function appendSearchParam(
  params: URLSearchParams,
  key: string,
  value: unknown
): void {
  if (value === null || value === undefined || value === '') {
    return
  }

  if (Array.isArray(value)) {
    value.forEach((item) => appendSearchParam(params, key, item))
    return
  }

  if (value instanceof Date) {
    params.append(key, value.toISOString())
    return
  }

  if (typeof value === 'object') {
    params.append(key, JSON.stringify(value))
    return
  }

  params.append(key, String(value))
}

function buildRowsSearchParams(request: ReadRowsRequest) {
  const params = new URLSearchParams()

  request.columns?.forEach((column) => params.append('columns', column))

  Object.entries(request.filter ?? {}).forEach(([field, operations]) => {
    Object.entries(operations).forEach(([operator, value]) => {
      appendSearchParam(params, `filter[${field}].${operator}`, value)
    })
  })

  request.sort?.forEach((sort, index) => {
    params.append(`sort[${index}].field`, sort.field)
    params.append(`sort[${index}].direction`, sort.direction)
  })

  appendSearchParam(params, 'limit', request.limit)
  appendSearchParam(params, 'offset', request.offset)

  return params.toString()
}

async function parseSchemaEnvelope(
  response: Response,
  fallback: string
): Promise<GeneratedSchema> {
  if (!response.ok) {
    throw await buildApiRequestError(response, fallback)
  }

  const body = await parseResponseBody<SchemaEnvelope>(response)
  if (!body || typeof body !== 'object' || !('schema' in body) || !body.schema) {
    throw new Error(fallback)
  }

  return body.schema
}

export async function getSchema(datasourceId: number): Promise<GeneratedSchema> {
  const response = await apiFetch(`/core/datasources/${datasourceId}/schema`, {
    method: 'GET',
  })

  return parseSchemaEnvelope(response, 'Loading schema failed')
}

export async function getSchemaSummary(
  datasourceId: number
): Promise<SchemaSummary> {
  const response = await apiFetch(`/core/datasources/${datasourceId}/schema/summary`, {
    method: 'GET',
  })

  if (!response.ok) {
    throw await buildApiRequestError(response, 'Loading schema summary failed')
  }

  const body = await parseResponseBody<SchemaSummary>(response)
  if (!body || typeof body !== 'object') {
    throw new Error('Loading schema summary failed')
  }

  return body as SchemaSummary
}

export async function listTables(datasourceId: number): Promise<SchemaTable[]> {
  const response = await apiFetch(`/core/datasources/${datasourceId}/tables`, {
    method: 'GET',
  })

  if (!response.ok) {
    throw await buildApiRequestError(response, 'Loading tables failed')
  }

  const body = await parseResponseBody<TablesEnvelope>(response)
  if (!body || typeof body !== 'object' || !Array.isArray(body.tables)) {
    throw new Error('Loading tables failed')
  }

  return body.tables
}

export async function getTable(
  datasourceId: number,
  tableName: string
): Promise<SchemaTable> {
  const response = await apiFetch(
    `/core/datasources/${datasourceId}/tables/${encodeTableName(tableName)}`,
    {
      method: 'GET',
    }
  )

  if (!response.ok) {
    throw await buildApiRequestError(response, 'Loading table failed')
  }

  const body = await parseResponseBody<TableEnvelope>(response)
  if (!body || typeof body !== 'object' || !body.table) {
    throw new Error('Loading table failed')
  }

  return body.table
}

export async function generateSchema(
  datasourceId: number
): Promise<GeneratedSchema> {
  const response = await apiFetch(
    `/core/datasources/${datasourceId}/schema/generate`,
    {
      method: 'POST',
    }
  )

  return parseSchemaEnvelope(response, 'Generating schema failed')
}

export async function refreshSchema(
  datasourceId: number
): Promise<GeneratedSchema> {
  const response = await apiFetch(
    `/core/datasources/${datasourceId}/schema/refresh`,
    {
      method: 'POST',
    }
  )

  return parseSchemaEnvelope(response, 'Refreshing schema failed')
}

export async function readRows(
  datasourceId: number,
  tableName: string,
  request: ReadRowsRequest
): Promise<RowsResponse> {
  const queryString = buildRowsSearchParams(request)
  const response = await apiFetch(
    `/core/datasources/${datasourceId}/tables/${encodeTableName(tableName)}/rows${
      queryString ? `?${queryString}` : ''
    }`,
    {
      method: 'GET',
    }
  )

  if (!response.ok) {
    throw await buildApiRequestError(response, 'Loading rows failed')
  }

  const body = await parseResponseBody<RowsResponse>(response)
  if (!body || typeof body !== 'object') {
    throw new Error('Loading rows failed')
  }

  return body as RowsResponse
}

export async function aggregateRows(
  datasourceId: number,
  tableName: string,
  request: AggregateRequest
): Promise<AggregateResponse> {
  const response = await apiFetch(
    `/core/datasources/${datasourceId}/tables/${encodeTableName(tableName)}/aggregate`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    }
  )

  if (!response.ok) {
    throw await buildApiRequestError(response, 'Running aggregate query failed')
  }

  const body = await parseResponseBody<AggregateResponse>(response)
  if (!body || typeof body !== 'object') {
    throw new Error('Running aggregate query failed')
  }

  return body as AggregateResponse
}

export async function createRow(
  datasourceId: number,
  tableName: string,
  request: CreateRowRequest
): Promise<CreateRowResponse> {
  const response = await apiFetch(
    `/core/datasources/${datasourceId}/tables/${encodeTableName(tableName)}/rows`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    }
  )

  if (!response.ok) {
    throw await buildApiRequestError(response, 'Creating row failed')
  }

  const body = await parseResponseBody<CreateRowResponse>(response)
  if (!body || typeof body !== 'object') {
    throw new Error('Creating row failed')
  }

  return body as CreateRowResponse
}

export async function updateRow(
  datasourceId: number,
  tableName: string,
  id: number,
  request: UpdateRowRequest
): Promise<UpdateRowResponse> {
  const response = await apiFetch(
    `/core/datasources/${datasourceId}/tables/${encodeTableName(tableName)}/rows/${id}`,
    {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    }
  )

  if (!response.ok) {
    throw await buildApiRequestError(response, 'Updating row failed')
  }

  const body = await parseResponseBody<UpdateRowResponse>(response)
  if (!body || typeof body !== 'object') {
    throw new Error('Updating row failed')
  }

  return body as UpdateRowResponse
}

export async function deleteRow(
  datasourceId: number,
  tableName: string,
  id: number
): Promise<DeleteRowResponse> {
  const response = await apiFetch(
    `/core/datasources/${datasourceId}/tables/${encodeTableName(tableName)}/rows/${id}`,
    {
      method: 'DELETE',
    }
  )

  if (!response.ok) {
    throw await buildApiRequestError(response, 'Deleting row failed')
  }

  const body = await parseResponseBody<DeleteRowResponse>(response)
  if (!body || typeof body !== 'object') {
    throw new Error('Deleting row failed')
  }

  return body as DeleteRowResponse
}
