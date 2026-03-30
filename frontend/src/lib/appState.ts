export type StoredDataSourceSummary = {
  name: string
  dbType: string
  host: string
  port: number
  databaseName: string
  username: string
  schemaName?: string | null
}

export type StoredDataSourceDetails = StoredDataSourceSummary & {
  encryptedPassword: string
  sslEnabled: boolean
  sslMode?: string | null
}

const SESSION_KEY = 'forgeql.session-active'
const DATASOURCE_KEY = 'forgeql.saved-datasource'

export function markSessionActive() {
  localStorage.setItem(SESSION_KEY, 'true')
}

export function hasLocalSession() {
  return localStorage.getItem(SESSION_KEY) === 'true'
}

export function storeSavedDatasource(summary: StoredDataSourceSummary) {
  localStorage.setItem(DATASOURCE_KEY, JSON.stringify(summary))
}

export function storeSavedDatasourceDetails(details: StoredDataSourceDetails) {
  localStorage.setItem(DATASOURCE_KEY, JSON.stringify(details))
}

export function clearSavedDatasource() {
  localStorage.removeItem(DATASOURCE_KEY)
}

export function hasSavedDatasource() {
  return localStorage.getItem(DATASOURCE_KEY) !== null
}

export function getStoredDatasourceSummary(): StoredDataSourceSummary | null {
  const raw = localStorage.getItem(DATASOURCE_KEY)

  if (!raw) {
    return null
  }

  try {
    return JSON.parse(raw) as StoredDataSourceSummary
  } catch {
    localStorage.removeItem(DATASOURCE_KEY)
    return null
  }
}

export function getStoredDatasourceDetails(): StoredDataSourceDetails | null {
  const summary = getStoredDatasourceSummary()

  if (!summary) {
    return null
  }

  return {
    ...summary,
    encryptedPassword:
      'encryptedPassword' in summary &&
      typeof summary.encryptedPassword === 'string'
        ? summary.encryptedPassword
        : '',
    sslEnabled:
      'sslEnabled' in summary && typeof summary.sslEnabled === 'boolean'
        ? summary.sslEnabled
        : false,
    sslMode:
      'sslMode' in summary && typeof summary.sslMode === 'string'
        ? summary.sslMode
        : null,
  }
}
