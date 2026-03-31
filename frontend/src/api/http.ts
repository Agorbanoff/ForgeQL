const API_BASE_URL = 'http://localhost:8080'

type RequestOptions = RequestInit & {
  retryOnUnauthorized?: boolean
}

export type ApiErrorShape = {
  timestamp?: string
  status?: number
  path?: string
  message?: string
  error?: string
}

export class ApiRequestError extends Error {
  status?: number
  path?: string
  timestamp?: string
  errorLabel?: string

  constructor(message: string, details?: ApiErrorShape) {
    super(message)
    this.name = 'ApiRequestError'
    this.status = details?.status
    this.path = details?.path
    this.timestamp = details?.timestamp
    this.errorLabel = details?.error
  }
}

async function refreshAccessToken() {
  const response = await fetch(`${API_BASE_URL}/token/refresh`, {
    method: 'POST',
    credentials: 'include',
  })

  return response.ok
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

export async function apiFetch(
  path: string,
  options: RequestOptions = {}
): Promise<Response> {
  const { retryOnUnauthorized = true, headers, ...rest } = options

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...rest,
    headers: {
      ...(headers ?? {}),
    },
    credentials: 'include',
  })

  if (response.status === 401 && retryOnUnauthorized) {
    const refreshed = await refreshAccessToken()

    if (!refreshed) {
      window.location.href = '/login'
      throw new Error('Unauthorized')
    }

    const retryResponse = await fetch(`${API_BASE_URL}${path}`, {
      ...rest,
      headers: {
        ...(headers ?? {}),
      },
      credentials: 'include',
    })

    if (retryResponse.status === 401) {
      window.location.href = '/login'
      throw new Error('Unauthorized')
    }

    return retryResponse
  }

  return response
}
