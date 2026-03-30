import { apiFetch, buildApiRequestError, parseResponseBody } from './http'

export type DataSourcePayload = {
  name: string
  dbType: 'POSTGRESQL' | 'MYSQL'
  host: string
  port: number
  databaseName: string
  username: string
  encryptedPassword: string
  schemaName?: string
  SslEnabled: boolean
  sslMode?: 'DISABLE' | 'REQUIRE' | 'VERIFY_CA' | 'VERIFY_FULL'
}

export type SavedDataSource = {
  id: number
  name: string
  dbType: string
  host: string
  port: number
  databaseName: string
  username: string
  schemaName?: string | null
  SslEnabled?: boolean
  sslEnabled?: boolean
  sslMode?: string | null
}

export async function saveDataSource(payload: DataSourcePayload): Promise<void> {
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

export async function getDataSources(): Promise<SavedDataSource[]> {
  const response = await apiFetch('/datasource', {
    method: 'GET',
  })

  if (!response.ok) {
    throw await buildApiRequestError(response, 'Loading datasources failed')
  }

  const body = await parseResponseBody<SavedDataSource[]>(response)
  return Array.isArray(body) ? body : []
}
