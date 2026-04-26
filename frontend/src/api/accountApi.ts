import { apiFetch, buildApiRequestError, initializeRequestSecurity } from './http'
import type { GlobalRole } from '../types/platform'

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
  username: string
  globalRole: GlobalRole
}

export async function signUpUser(payload: SignUpPayload): Promise<void> {
  const response = await apiFetch('/account/signup', {
    method: 'POST',
    retryOnUnauthorized: false,
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      username: payload.username.trim(),
      email: payload.email.trim(),
      password: payload.password,
    }),
  })

  if (!response.ok) {
    throw await buildApiRequestError(response, 'Sign up failed')
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

  await initializeRequestSecurity()
}

export async function logOutUser(): Promise<void> {
  const response = await apiFetch('/account/logout', {
    method: 'POST',
  })

  if (!response.ok) {
    throw await buildApiRequestError(response, 'Log out failed')
  }
}

export async function deleteCurrentAccount(): Promise<void> {
  const response = await apiFetch('/account/me', {
    method: 'DELETE',
  })

  if (!response.ok) {
    throw await buildApiRequestError(response, 'Deleting account failed')
  }
}

export async function getCurrentUser(): Promise<CurrentUser> {
  const response = await apiFetch('/account/me', {
    method: 'GET',
  })

  if (!response.ok) {
    throw await buildApiRequestError(response, 'Loading current user failed')
  }

  const body = await response.json()
  return body as CurrentUser
}
