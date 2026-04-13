const rawBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim()
const API_BASE_URL = (rawBaseUrl || '/api').replace(/\/$/, '')
const CSRF_COOKIE_NAME = 'XSRF-TOKEN'
const CSRF_HEADER_NAME = 'X-XSRF-TOKEN'
export const AUTH_REQUIRED_EVENT = 'forgeql:auth-required'
const CSRF_EXEMPT_PATHS = new Set([
  '/account/signup',
  '/account/login',
  '/account/logout',
  '/token/refresh',
])

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
    const headers = await buildRequestHeaders('/token/refresh', { method: 'POST' })
    const response = await fetch(`${API_BASE_URL}/token/refresh`, {
      method: 'POST',
      headers,
      credentials: 'include',
    })

    return response.ok
  } catch {
    return false
  }
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

function isAccessDeniedError(body: ApiErrorShape | string | null) {
  if (!body || typeof body === 'string') {
    return false
  }

  return body.status === 403 || body.code === 'ACCESS_DENIED'
}

async function fetchCsrfToken() {
  const response = await fetch(`${API_BASE_URL}/account/csrf`, {
    method: 'GET',
    credentials: 'include',
  })

  return response.ok
}

export async function initializeRequestSecurity() {
  await ensureCsrfToken()
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

  if (
    path !== '/account/csrf' &&
    !CSRF_EXEMPT_PATHS.has(path) &&
    isMutatingMethod(options.method)
  ) {
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

function dispatchAuthRequired() {
  window.dispatchEvent(new Event(AUTH_REQUIRED_EVENT))
}

export async function apiFetch(
  path: string,
  options: RequestOptions = {}
): Promise<Response> {
  const { retryOnUnauthorized = true, headers, ...rest } = options
  let requestHeaders: Headers

  try {
    requestHeaders = await buildRequestHeaders(path, {
      headers,
      method: rest.method,
    })
  } catch (error) {
    if (error instanceof ApiRequestError) {
      throw error
    }

    throw new ApiRequestError(
      error instanceof Error ? error.message : 'Could not prepare the request.'
    )
  }

  let response: Response

  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...rest,
      headers: requestHeaders,
      credentials: 'include',
    })
  } catch (error) {
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

    const retryHeaders = await buildRequestHeaders(path, {
      headers,
      method: rest.method,
    })

    let retryResponse: Response

    try {
      retryResponse = await fetch(`${API_BASE_URL}${path}`, {
        ...rest,
        headers: retryHeaders,
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

  if (response.status === 403 && isMutatingMethod(rest.method)) {
    const errorBody = await parseResponseBody<ApiErrorShape>(response.clone())

    if (isAccessDeniedError(errorBody)) {
      await fetchCsrfToken()

      const retryHeaders = await buildRequestHeaders(path, {
        headers,
        method: rest.method,
      })

      try {
        return await fetch(`${API_BASE_URL}${path}`, {
          ...rest,
          headers: retryHeaders,
          credentials: 'include',
        })
      } catch {
        throw new ApiRequestError(
          'Cannot reach the backend. Check that the API server is running and that the frontend proxy or API base URL is correct.'
        )
      }
    }
  }

  return response
}
