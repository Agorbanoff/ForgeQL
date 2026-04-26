import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import {
  createApiErrorResponse,
  createJsonResponse,
  installFetchMock,
} from '../utils/fetchMock'
import { buildCurrentUser } from '../utils/fixtures'
import { renderAppAt } from '../utils/render'

describe('auth flow', () => {
  it('submits the login form and routes into datasource management on success', async () => {
    const fetchMock = installFetchMock()
    const user = buildCurrentUser()
    let meCalls = 0

    fetchMock.route('GET', '/api/account/me', () => {
      meCalls += 1
      return meCalls === 1
        ? createApiErrorResponse(
            401,
            'AUTHENTICATION_REQUIRED',
            'Authentication is required',
            '/api/account/me'
          )
        : createJsonResponse(user)
    })
    fetchMock.route('POST', '/api/account/login', createJsonResponse({}))
    fetchMock.route('GET', '/api/datasource', createJsonResponse([]))

    renderAppAt('/login')

    const actor = userEvent.setup()
    await actor.type(
      await screen.findByPlaceholderText('name@example.com'),
      'user@example.com'
    )
    await actor.type(screen.getByPlaceholderText('Password'), 'secret123')
    await actor.click(screen.getByRole('button', { name: 'Continue' }))

    expect(await screen.findByText('Accessible datasources')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Refresh datasources' })).toBeInTheDocument()

    const loginCall = fetchMock.getCalls('POST', '/api/account/login')[0]
    expect(loginCall?.bodyJson).toEqual({
      email: 'user@example.com',
      password: 'secret123',
    })
  })

  it('shows the backend login error when credentials are rejected', async () => {
    const fetchMock = installFetchMock()

    fetchMock.route(
      'GET',
      '/api/account/me',
      createApiErrorResponse(
        401,
        'AUTHENTICATION_REQUIRED',
        'Authentication is required',
        '/api/account/me'
      )
    )
    fetchMock.route(
      'POST',
      '/api/account/login',
      createApiErrorResponse(
        401,
        'AUTHENTICATION_REQUIRED',
        'Invalid email or password',
        '/api/account/login'
      )
    )

    renderAppAt('/login')

    const actor = userEvent.setup()
    await actor.type(
      await screen.findByPlaceholderText('name@example.com'),
      'user@example.com'
    )
    await actor.type(screen.getByPlaceholderText('Password'), 'wrong-password')
    await actor.click(screen.getByRole('button', { name: 'Continue' }))

    expect(await screen.findByText('Invalid email or password')).toBeInTheDocument()
  })

  it('redirects unauthenticated users to login before opening protected routes', async () => {
    const fetchMock = installFetchMock()

    fetchMock.route(
      'GET',
      '/api/account/me',
      createApiErrorResponse(
        401,
        'AUTHENTICATION_REQUIRED',
        'Authentication is required',
        '/api/account/me'
      )
    )

    renderAppAt('/datasource')

    expect(await screen.findByText('Log in')).toBeInTheDocument()
    expect(
      screen.getByText(/Authentication opens the secured console/i)
    ).toBeInTheDocument()
  })

  it('lets authenticated users access protected routes', async () => {
    const fetchMock = installFetchMock()

    fetchMock.route('GET', '/api/account/me', createJsonResponse(buildCurrentUser()))
    fetchMock.route('GET', '/api/datasource', createJsonResponse([]))

    renderAppAt('/datasource')

    await waitFor(() => {
      expect(screen.getByText('Accessible datasources')).toBeInTheDocument()
    })
  })
})
