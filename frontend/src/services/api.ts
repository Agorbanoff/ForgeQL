import { apiFetch, buildApiRequestError, parseResponseBody } from '../api/http'
import type {
  AdminUserRecord,
  AssignDatasourceAccessPayload,
  DatasourceAccessRecord,
  DatasourceRecord,
  GlobalRole,
  UpdateDatasourceAccessPayload,
} from '../types/platform'

async function parseJsonResponse<T>(
  response: Response,
  fallback: string
): Promise<T> {
  if (!response.ok) {
    throw await buildApiRequestError(response, fallback)
  }

  const body = await parseResponseBody<T>(response)
  if (body == null) {
    throw new Error(fallback)
  }

  return body as T
}

async function runVoidRequest(response: Response, fallback: string) {
  if (!response.ok) {
    throw await buildApiRequestError(response, fallback)
  }
}

export async function getDatasources(): Promise<DatasourceRecord[]> {
  const response = await apiFetch('/datasource', { method: 'GET' })

  if (!response.ok) {
    throw await buildApiRequestError(response, 'Loading datasources failed')
  }

  const body = await parseResponseBody<DatasourceRecord[]>(response)
  return Array.isArray(body) ? body : []
}

export async function getDatasourceAccessList(
  datasourceId: number
): Promise<DatasourceAccessRecord[]> {
  const response = await apiFetch(`/admin/datasources/${datasourceId}/access`, {
    method: 'GET',
  })

  const body = await parseJsonResponse<DatasourceAccessRecord[]>(
    response,
    'Loading datasource access failed'
  )

  return Array.isArray(body) ? body : []
}

export async function assignDatasourceAccess(
  datasourceId: number,
  payload: AssignDatasourceAccessPayload
): Promise<void> {
  const response = await apiFetch(`/admin/datasources/${datasourceId}/access`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  })

  await runVoidRequest(response, 'Assigning datasource access failed')
}

export async function updateDatasourceAccess(
  datasourceId: number,
  userId: number,
  payload: UpdateDatasourceAccessPayload
): Promise<void> {
  const response = await apiFetch(
    `/admin/datasources/${datasourceId}/access/${userId}`,
    {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    }
  )

  await runVoidRequest(response, 'Updating datasource access failed')
}

export async function deleteDatasourceAccess(
  datasourceId: number,
  userId: number
): Promise<void> {
  const response = await apiFetch(
    `/admin/datasources/${datasourceId}/access/${userId}`,
    {
      method: 'DELETE',
    }
  )

  await runVoidRequest(response, 'Removing datasource access failed')
}

export async function getAdminUsers(): Promise<AdminUserRecord[]> {
  const response = await apiFetch('/admin/users', {
    method: 'GET',
  })

  const body = await parseJsonResponse<AdminUserRecord[]>(
    response,
    'Loading users failed'
  )

  return Array.isArray(body) ? body : []
}

export async function updateUserRole(
  userId: number,
  globalRole: GlobalRole
): Promise<void> {
  const response = await apiFetch(`/admin/users/${userId}/role`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ globalRole }),
  })

  await runVoidRequest(response, 'Updating user role failed')
}
