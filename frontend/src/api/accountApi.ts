import { apiFetch } from './http'

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
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  })

  if (!response.ok) {
    const text = await response.text()
    throw new Error(text || 'Sign up failed')
  }
}

export async function logInUser(payload: LogInPayload): Promise<void> {
  const response = await apiFetch('/account/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  })

  if (!response.ok) {
    const text = await response.text()
    throw new Error(text || 'Log in failed')
  }
}