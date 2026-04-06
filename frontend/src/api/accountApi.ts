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
}
