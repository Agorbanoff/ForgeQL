const API_BASE_URL = 'http://localhost:8080'
const CSRF_COOKIE_NAME = 'XSRF-TOKEN'
const CSRF_HEADER_NAME = 'X-XSRF-TOKEN'

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
  const headers = await buildRequestHeaders('/token/refresh', { method: 'POST' })
  const response = await fetch(`${API_BASE_URL}/token/refresh`, {
    method: 'POST',
    headers,
    credentials: 'include',
  })

  return response.ok
}

function getCookie(name: string): string | null {
  const encodedName = `${encodeURIComponent(name)}=`
  const parts = document.cookie.split(';')

  for (const part of parts) {
    const trimmed = part.trim()
    if (trimmed.startsWith(encodedName)) {
      return decodeURIComponent(trimmed.slice(encodedName.length))
    }
  }

  return null
}

function isMutatingMethod(method?: string) {
  const normalized = (method ?? 'GET').toUpperCase()
  return !['GET', 'HEAD', 'OPTIONS', 'TRACE'].includes(normalized)
}

async function fetchCsrfToken() {
  const response = await fetch(`${API_BASE_URL}/account/csrf`, {
    method: 'GET',
    credentials: 'include',
  })

  return response.ok
}

async function ensureCsrfToken() {
  if (getCookie(CSRF_COOKIE_NAME)) {
    return
  }

  const loaded = await fetchCsrfToken()
  if (!loaded || !getCookie(CSRF_COOKIE_NAME)) {
    throw new Error('Could not initialize request security.')
  }
}

async function buildRequestHeaders(
  path: string,
  options: Pick<RequestOptions, 'headers' | 'method'>
) {
  const headers = new Headers(options.headers ?? {})

  if (path !== '/account/csrf' && isMutatingMethod(options.method)) {
    await ensureCsrfToken()
    const csrfToken = getCookie(CSRF_COOKIE_NAME)

    if (!csrfToken) {
      throw new Error('Could not initialize request security.')
    }

    headers.set(CSRF_HEADER_NAME, csrfToken)
  }

  return headers
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
  const requestHeaders = await buildRequestHeaders(path, {
    headers,
    method: rest.method,
  })

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...rest,
    headers: requestHeaders,
    credentials: 'include',
  })

  if (response.status === 401 && retryOnUnauthorized && path !== '/token/refresh') {
    const refreshed = await refreshAccessToken()

    if (!refreshed) {
      localStorage.removeItem('sigmaql.session-active')
      window.location.href = '/login'
      throw new Error('Unauthorized')
    }

    const retryHeaders = await buildRequestHeaders(path, {
      headers,
      method: rest.method,
    })

    const retryResponse = await fetch(`${API_BASE_URL}${path}`, {
      ...rest,
      headers: retryHeaders,
      credentials: 'include',
    })

    if (retryResponse.status === 401) {
      localStorage.removeItem('sigmaql.session-active')
      window.location.href = '/login'
      throw new Error('Unauthorized')
    }

    return retryResponse
  }

  return response
}
