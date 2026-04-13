import { apiFetch, buildApiRequestError, parseResponseBody } from './http'
import type {
  DatasourceConnectionTestResult,
  DatasourcePayload,
  DatasourceRecord,
} from '../types/platform'

async function parseDatasourceResponse(
  response: Response,
  fallback: string
): Promise<DatasourceRecord> {
  if (!response.ok) {
    throw await buildApiRequestError(response, fallback)
  }

  const body = await parseResponseBody<DatasourceRecord>(response)
  if (!body || typeof body !== 'object') {
    throw new Error(fallback)
  }

  return body as DatasourceRecord
}

export async function listDataSources(): Promise<DatasourceRecord[]> {
  const response = await apiFetch('/datasource', {
    method: 'GET',
  })

  if (!response.ok) {
    throw await buildApiRequestError(response, 'Loading datasources failed')
  }

  const body = await parseResponseBody<DatasourceRecord[]>(response)
  return Array.isArray(body) ? body : []
}

export async function getDataSource(id: number): Promise<DatasourceRecord> {
  const response = await apiFetch(`/datasource/${id}`, {
    method: 'GET',
  })

  return parseDatasourceResponse(response, 'Loading datasource failed')
}

export async function createDataSource(payload: DatasourcePayload): Promise<void> {
  const response = await apiFetch('/datasource', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  })

  if (!response.ok) {
    throw await buildApiRequestError(response, 'Saving datasource failed')
  }
}

export async function updateDataSource(
  id: number,
  payload: DatasourcePayload
): Promise<void> {
  const response = await apiFetch(`/datasource/${id}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  })

  if (!response.ok) {
    throw await buildApiRequestError(response, 'Updating datasource failed')
  }
}

export async function deleteDataSource(id: number): Promise<void> {
  const response = await apiFetch(`/datasource/${id}`, {
    method: 'DELETE',
  })

  if (!response.ok) {
    throw await buildApiRequestError(response, 'Deleting datasource failed')
  }
}

export async function testDataSourceConnection(
  id: number
): Promise<DatasourceConnectionTestResult> {
  const response = await apiFetch(`/datasource/${id}/test-connection`, {
    method: 'POST',
  })

  if (!response.ok) {
    throw await buildApiRequestError(response, 'Testing datasource failed')
  }

  const body = await parseResponseBody<DatasourceConnectionTestResult>(response)
  if (!body || typeof body !== 'object') {
    throw new Error('Testing datasource failed')
  }

  return body as DatasourceConnectionTestResult
}
