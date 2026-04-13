import type { DatabaseType } from '../types/platform'

export type StoredDatasourceSelection = {
  id: number
  displayName: string
  dbType: DatabaseType
  host: string
  port: number
  databaseName: string
  schemaName: string
  username: string
  lastSchemaGeneratedAt?: string | null
  lastSchemaFingerprint?: string | null
}

export type StoredDataSourceSummary = StoredDatasourceSelection

const DATASOURCE_KEY = 'sigmaql.selected-datasource'

function safeParseSelection(raw: string): StoredDatasourceSelection | null {
  try {
    const parsed = JSON.parse(raw) as Partial<StoredDatasourceSelection>

    if (
      typeof parsed.id !== 'number' ||
      typeof parsed.displayName !== 'string' ||
      typeof parsed.dbType !== 'string' ||
      typeof parsed.host !== 'string' ||
      typeof parsed.port !== 'number' ||
      typeof parsed.databaseName !== 'string' ||
      typeof parsed.schemaName !== 'string' ||
      typeof parsed.username !== 'string'
    ) {
      return null
    }

    return {
      id: parsed.id,
      displayName: parsed.displayName,
      dbType: parsed.dbType as DatabaseType,
      host: parsed.host,
      port: parsed.port,
      databaseName: parsed.databaseName,
      schemaName: parsed.schemaName,
      username: parsed.username,
      lastSchemaGeneratedAt:
        typeof parsed.lastSchemaGeneratedAt === 'string'
          ? parsed.lastSchemaGeneratedAt
          : null,
      lastSchemaFingerprint:
        typeof parsed.lastSchemaFingerprint === 'string'
          ? parsed.lastSchemaFingerprint
          : null,
    }
  } catch {
    return null
  }
}

export function storeSelectedDatasource(selection: StoredDatasourceSelection) {
  localStorage.setItem(DATASOURCE_KEY, JSON.stringify(selection))
}

export function clearSavedDatasource() {
  localStorage.removeItem(DATASOURCE_KEY)
}

export function hasSavedDatasource() {
  const raw = localStorage.getItem(DATASOURCE_KEY)

  if (!raw) {
    return false
  }

  const parsed = safeParseSelection(raw)
  if (!parsed) {
    localStorage.removeItem(DATASOURCE_KEY)
    return false
  }

  return true
}

export function getStoredDatasourceDetails(): StoredDatasourceSelection | null {
  const raw = localStorage.getItem(DATASOURCE_KEY)

  if (!raw) {
    return null
  }

  const parsed = safeParseSelection(raw)
  if (!parsed) {
    localStorage.removeItem(DATASOURCE_KEY)
    return null
  }

  return parsed
}
