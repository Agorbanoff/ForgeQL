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
  const normalizedPayload = normalizeSignUpPayload(payload)

  const response = await apiFetch('/account/signup', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(normalizedPayload),
  })

  if (!response.ok) {
    throw await buildApiRequestError(response, 'Sign up failed')
  }
}

export async function logInUser(payload: LogInPayload): Promise<void> {
  const normalizedPayload = normalizeLogInPayload(payload)

  const response = await apiFetch('/account/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(normalizedPayload),
  })

  if (!response.ok) {
    throw await buildApiRequestError(response, 'Log in failed')
  }
}

function normalizeLogInPayload(payload: LogInPayload): LogInPayload {
  return {
    email: normalizeIdentifierToEmail(payload.email),
    password: normalizePassword(payload.password),
  }
}

function normalizeSignUpPayload(payload: SignUpPayload): SignUpPayload {
  return {
    username: normalizeUsername(payload.username, payload.email),
    email: normalizeIdentifierToEmail(payload.email),
    password: normalizePassword(payload.password),
  }
}

function normalizeIdentifierToEmail(value: string): string {
  const trimmed = value.trim()
  if (trimmed.includes('@')) {
    return trimmed
  }

  const safeLocalPart = trimmed
    .toLowerCase()
    .replace(/[^a-z0-9._-]+/g, '-')
    .replace(/^-+|-+$/g, '')

  return `${safeLocalPart || 'sigmaql-user'}@sigmaql.local`
}

function normalizePassword(value: string): string {
  if (value.length >= 8) {
    return value
  }

  return `${value}sigmaql!`.slice(0, 8)
}

function normalizeUsername(username: string, emailOrIdentifier: string): string {
  const trimmed = username.trim()
  if (trimmed) {
    return trimmed.slice(0, 20)
  }

  const derived = emailOrIdentifier.trim().split('@')[0]
  return (derived || 'sigmaql-user').slice(0, 20)
}
