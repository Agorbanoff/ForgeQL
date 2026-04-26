const rawBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim()
const API_BASE_URL = (rawBaseUrl || '/api').replace(/\/$/, '')

export const AUTH_REQUIRED_EVENT = 'forgeql:auth-required'

type RequestOptions = RequestInit & {
  retryOnUnauthorized?: boolean
}

export type ApiErrorShape = {
  timestamp?: string
  status?: number
  error?: string
  code?: string
  message?: string
  datasourceId?: number
  targetPath?: string
}

export class ApiRequestError extends Error {
  status?: number
  timestamp?: string
  errorLabel?: string
  code?: string
  datasourceId?: number
  targetPath?: string

  constructor(message: string, details?: ApiErrorShape) {
    super(message)
    this.name = 'ApiRequestError'
    this.status = details?.status
    this.timestamp = details?.timestamp
    this.errorLabel = details?.error
    this.code = details?.code
    this.datasourceId = details?.datasourceId
    this.targetPath = details?.targetPath
  }
}

async function refreshAccessToken() {
  try {
    const response = await fetch(`${API_BASE_URL}/token/refresh`, {
      method: 'POST',
      credentials: 'include',
    })

    return response.ok
  } catch {
    return false
  }
}

export async function initializeRequestSecurity() {
  return
}

export async function parseResponseBody<T = unknown>(
  response: Response
): Promise<T | string | null> {
  const text = await response.text()

  if (!text) {
    return null
  }

  try {
    return JSON.parse(text) as T
  } catch {
    return text
  }
}

export async function getErrorMessage(
  response: Response,
  fallback: string
): Promise<string> {
  const body = await parseResponseBody<ApiErrorShape>(response)

  if (typeof body === 'string' && body.trim()) {
    return body
  }

  if (body && typeof body === 'object') {
    if (typeof body.message === 'string' && body.message.trim()) {
      return body.message
    }

    if (typeof body.error === 'string' && body.error.trim()) {
      return body.error
    }
  }

  return fallback
}

export async function buildApiRequestError(
  response: Response,
  fallback: string
): Promise<ApiRequestError> {
  const body = await parseResponseBody<ApiErrorShape>(response)

  if (body && typeof body === 'object') {
    const message =
      typeof body.message === 'string' && body.message.trim()
        ? body.message
        : typeof body.error === 'string' && body.error.trim()
          ? body.error
          : fallback

    return new ApiRequestError(message, body)
  }

  if (typeof body === 'string' && body.trim()) {
    return new ApiRequestError(body)
  }

  return new ApiRequestError(fallback, { status: response.status })
}

function dispatchAuthRequired() {
  window.dispatchEvent(new Event(AUTH_REQUIRED_EVENT))
}

export async function apiFetch(
  path: string,
  options: RequestOptions = {}
): Promise<Response> {
  const { retryOnUnauthorized = true, ...rest } = options

  let response: Response

  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...rest,
      credentials: 'include',
    })
  } catch {
    throw new ApiRequestError(
      'Cannot reach the backend. Check that the API server is running and that the frontend proxy or API base URL is correct.'
    )
  }

  if (response.status === 401 && retryOnUnauthorized && path !== '/token/refresh') {
    const refreshed = await refreshAccessToken()

    if (!refreshed) {
      dispatchAuthRequired()
      throw await buildApiRequestError(response, 'Authentication is required')
    }

    let retryResponse: Response

    try {
      retryResponse = await fetch(`${API_BASE_URL}${path}`, {
        ...rest,
        credentials: 'include',
      })
    } catch {
      throw new ApiRequestError(
        'Cannot reach the backend. Check that the API server is running and that the frontend proxy or API base URL is correct.'
      )
    }

    if (retryResponse.status === 401) {
      dispatchAuthRequired()
      throw await buildApiRequestError(retryResponse, 'Authentication is required')
    }

    return retryResponse
  }

  return response
}
