import { apiFetch, buildApiRequestError } from './http'

export type SignUpPayload = {
  username: string
  email: string
  password: string
}

export type LogInPayload = {
  email: string
  password: string
}

export type CurrentUser = {
  id: number
  email: string
}

export async function signUpUser(payload: SignUpPayload): Promise<void> {
  const requestBody = {
    username: payload.username.trim(),
    email: payload.email.trim(),
    password: payload.password,
  }

  console.info('[auth][signup] submitting request', {
    username: requestBody.username,
    email: requestBody.email,
    passwordLength: requestBody.password.length,
  })

  try {
    const response = await apiFetch('/account/signup', {
      method: 'POST',
      retryOnUnauthorized: false,
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(requestBody),
    })

    if (!response.ok) {
      const error = await buildApiRequestError(response, 'Sign up failed')

      console.error('[auth][signup] request failed', {
        status: error.status ?? response.status,
        path: error.path ?? '/account/signup',
        timestamp: error.timestamp,
        errorLabel: error.errorLabel,
        message: error.message,
      })

      throw error
    }

    console.info('[auth][signup] request succeeded', {
      status: response.status,
    })
  } catch (error) {
    console.error('[auth][signup] unexpected error', error)
    throw error
  }
}

export async function logInUser(payload: LogInPayload): Promise<void> {
  const response = await apiFetch('/account/login', {
    method: 'POST',
    retryOnUnauthorized: false,
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      email: payload.email.trim(),
      password: payload.password,
    }),
  })

  if (!response.ok) {
    throw await buildApiRequestError(response, 'Log in failed')
  }
}

export async function logOutUser(): Promise<void> {
  const response = await apiFetch('/account/logout', {
    method: 'POST',
  })

  if (!response.ok) {
    throw await buildApiRequestError(response, 'Log out failed')
  }
}

export async function getCurrentUser(): Promise<CurrentUser> {
  const response = await apiFetch('/account/me', {
    method: 'GET',
    retryOnUnauthorized: false,
  })

  if (!response.ok) {
    throw await buildApiRequestError(response, 'Loading current user failed')
  }

  const body = await response.json()
  return body as CurrentUser
}
