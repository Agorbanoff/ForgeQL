const API_BASE_URL = 'http://localhost:8080'

type RequestOptions = RequestInit & {
  retryOnUnauthorized?: boolean
}

async function refreshAccessToken() {
  const response = await fetch(`${API_BASE_URL}/token/refresh`, {
    method: 'POST',
    credentials: 'include',
  })

  return response.ok
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