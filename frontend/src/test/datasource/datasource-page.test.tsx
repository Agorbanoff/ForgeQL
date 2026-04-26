import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import {
  createApiErrorResponse,
  createJsonResponse,
  installFetchMock,
} from '../utils/fetchMock'
import { buildCurrentUser, buildDatasource } from '../utils/fixtures'
import { renderAppAt } from '../utils/render'

const adminUsersResponse = [
  {
    id: 100,
    username: 'forgeql-owner',
    email: 'owner@forgeql.com',
    globalRole: 'MAIN_ADMIN',
    createdAt: '2026-04-13T11:00:00.000Z',
  },
  {
    id: 101,
    username: 'member1',
    email: 'member1@test.com',
    globalRole: 'MEMBER',
    createdAt: '2026-04-13T11:30:00.000Z',
  },
] as const

describe('datasource catalog', () => {
  it('renders datasource records returned by the backend', async () => {
    const fetchMock = installFetchMock()
    const datasource = buildDatasource({
      id: 7,
      displayName: 'Warehouse',
      host: 'warehouse.db',
    })

    fetchMock.route('GET', '/api/account/me', createJsonResponse(buildCurrentUser()))
    fetchMock.route('GET', '/api/datasource', createJsonResponse([datasource]))

    renderAppAt('/datasource')

    expect(await screen.findByText('Warehouse')).toBeInTheDocument()
    expect(screen.getByText('Role state')).toBeInTheDocument()
  })

  it('keeps viewer catalog controls read-only and hides the signed-in summary block', async () => {
    const fetchMock = installFetchMock()
    const viewerDatasource = buildDatasource({
      id: 8,
      displayName: 'Warehouse',
      accessRole: 'VIEWER',
    })

    fetchMock.route(
      'GET',
      '/api/account/me',
      createJsonResponse(buildCurrentUser({ username: 'viewer1', globalRole: 'VIEWER' }))
    )
    fetchMock.route('GET', '/api/datasource', createJsonResponse([viewerDatasource]))

    renderAppAt('/datasource')

    expect(await screen.findByText('Warehouse')).toBeInTheDocument()
    expect(screen.queryByText('Signed in as')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Open explorer' })).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Edit datasource' })
    ).not.toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Delete datasource' })
    ).not.toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Manage access' })
    ).not.toBeInTheDocument()
  })

  it('shows the empty datasource state when the backend returns no records', async () => {
    const fetchMock = installFetchMock()

    fetchMock.route('GET', '/api/account/me', createJsonResponse(buildCurrentUser()))
    fetchMock.route('GET', '/api/datasource', createJsonResponse([]))

    renderAppAt('/datasource')

    expect(
      await screen.findByText(/No datasources are available for this account yet/i)
    ).toBeInTheDocument()
  })

  it('shows the backend error when datasource loading fails', async () => {
    const fetchMock = installFetchMock()

    fetchMock.route('GET', '/api/account/me', createJsonResponse(buildCurrentUser()))
    fetchMock.route(
      'GET',
      '/api/datasource',
      createApiErrorResponse(
        500,
        'DATABASE_OPERATION_FAILED',
        'Could not load datasources.',
        '/api/datasource'
      )
    )

    renderAppAt('/datasource')

    expect(await screen.findByText('Could not load datasources.')).toBeInTheDocument()
  })

  it('updates a datasource from the catalog modal', async () => {
    const fetchMock = installFetchMock()
    const datasource = buildDatasource({
      id: 11,
      displayName: 'Warehouse',
      host: 'warehouse.db',
      username: 'forgeql_app',
    })
    const nextDatasource = {
      ...datasource,
      displayName: 'Warehouse primary',
      host: 'db.internal.example',
      username: 'service_user',
    }
    const updateBodies: unknown[] = []
    let datasourceCalls = 0

    fetchMock.route(
      'GET',
      '/api/account/me',
      createJsonResponse(buildCurrentUser({ globalRole: 'MAIN_ADMIN' }))
    )
    fetchMock.route('GET', '/api/admin/users', createJsonResponse(adminUsersResponse))
    fetchMock.route('GET', '/api/datasource', () => {
      datasourceCalls += 1
      return createJsonResponse(datasourceCalls === 1 ? [datasource] : [nextDatasource])
    })
    fetchMock.route('PUT', '/api/datasource/11', (request) => {
      updateBodies.push(request.bodyJson)
      return createJsonResponse({})
    })

    renderAppAt('/datasource')

    const actor = userEvent.setup()
    await actor.click(await screen.findByRole('button', { name: 'Update datasource' }))

    const dialog = await screen.findByText('Datasource update')
    const modal = dialog.closest('.surface-panel')
    expect(modal).not.toBeNull()

    await actor.clear(within(modal as HTMLElement).getByDisplayValue('Warehouse'))
    await actor.type(
      within(modal as HTMLElement).getByLabelText('Display name'),
      'Warehouse primary'
    )
    await actor.clear(within(modal as HTMLElement).getByDisplayValue('warehouse.db'))
    await actor.type(
      within(modal as HTMLElement).getByLabelText('Host'),
      'db.internal.example'
    )
    await actor.clear(within(modal as HTMLElement).getByDisplayValue('forgeql_app'))
    await actor.type(
      within(modal as HTMLElement).getByLabelText('Username'),
      'service_user'
    )
    await actor.click(
      within(modal as HTMLElement).getByRole('button', { name: 'Update datasource' })
    )

    expect(await screen.findByText('Warehouse was updated successfully.')).toBeInTheDocument()
    expect(await screen.findByText('Warehouse primary')).toBeInTheDocument()
    expect(updateBodies[0]).toEqual({
      displayName: 'Warehouse primary',
      dbType: 'POSTGRESQL',
      host: 'db.internal.example',
      port: 5432,
      databaseName: 'postgres',
      schemaName: 'public',
      username: 'service_user',
      password: null,
      sslMode: 'PREFER',
      connectTimeoutMs: 5000,
      socketTimeoutMs: 5000,
      applicationName: 'ForgeQL',
      sslRootCertRef: null,
      extraJdbcOptionsJson: null,
    })
  })

  it('deletes a datasource from the catalog modal', async () => {
    const fetchMock = installFetchMock()
    const datasource = buildDatasource({
      id: 4,
      displayName: 'Warehouse',
    })
    let datasourceCalls = 0

    fetchMock.route(
      'GET',
      '/api/account/me',
      createJsonResponse(buildCurrentUser({ globalRole: 'MAIN_ADMIN' }))
    )
    fetchMock.route('GET', '/api/admin/users', createJsonResponse(adminUsersResponse))
    fetchMock.route('GET', '/api/datasource', () => {
      datasourceCalls += 1
      return createJsonResponse(datasourceCalls === 1 ? [datasource] : [])
    })
    fetchMock.route('DELETE', '/api/datasource/4', new Response(null, { status: 204 }))

    renderAppAt('/datasource')

    const actor = userEvent.setup()
    await actor.click(await screen.findByRole('button', { name: 'Delete datasource' }))
    const modalLabel = await screen.findByText('Datasource deletion')
    const modal = modalLabel.closest('.surface-panel')
    expect(modal).not.toBeNull()
    await actor.click(
      within(modal as HTMLElement).getByRole('button', {
        name: /^Delete datasource$/,
      })
    )

    expect(await screen.findByText('Warehouse was deleted.')).toBeInTheDocument()
    expect(
      await screen.findByText(/No datasources are available for this account yet/i)
    ).toBeInTheDocument()
  })

  it('shows feedback when datasources are manually refreshed', async () => {
    const fetchMock = installFetchMock()
    const datasource = buildDatasource({
      id: 12,
      displayName: 'Warehouse',
    })
    let datasourceCalls = 0

    fetchMock.route('GET', '/api/account/me', createJsonResponse(buildCurrentUser()))
    fetchMock.route('GET', '/api/datasource', () => {
      datasourceCalls += 1
      return createJsonResponse([datasource])
    })

    renderAppAt('/datasource')

    const actor = userEvent.setup()
    await screen.findByText('Warehouse')
    await actor.click(screen.getByRole('button', { name: 'Refresh datasources' }))

    expect(await screen.findByText('Datasources refreshed.')).toBeInTheDocument()
    expect(datasourceCalls).toBe(2)
  })

  it('restores datasource creation for the main admin only', async () => {
    const fetchMock = installFetchMock()
    const createdDatasource = buildDatasource({
      id: 15,
      displayName: 'New warehouse',
      host: 'db.internal.example',
      databaseName: 'warehouse',
      username: 'forgeql_app',
    })
    const postedBodies: unknown[] = []
    let datasourceCalls = 0

    fetchMock.route(
      'GET',
      '/api/account/me',
      createJsonResponse(buildCurrentUser({ globalRole: 'MAIN_ADMIN' }))
    )
    fetchMock.route('GET', '/api/admin/users', createJsonResponse(adminUsersResponse))
    fetchMock.route('GET', '/api/datasource', () => {
      datasourceCalls += 1
      return createJsonResponse(datasourceCalls === 1 ? [] : [createdDatasource])
    })
    fetchMock.route('POST', '/api/datasource', (request) => {
      postedBodies.push(request.bodyJson)
      return createJsonResponse({}, { status: 201 })
    })

    renderAppAt('/datasource')

    const actor = userEvent.setup()
    await actor.click(await screen.findByRole('button', { name: 'Add datasource' }))

    const modalLabel = await screen.findByText('Datasource setup')
    const modal = modalLabel.closest('.surface-panel')
    expect(modal).not.toBeNull()

    await actor.type(
      within(modal as HTMLElement).getByPlaceholderText('Production analytics'),
      'New warehouse'
    )
    await actor.type(
      within(modal as HTMLElement).getByPlaceholderText('db.internal.example'),
      'db.internal.example'
    )
    await actor.type(
      within(modal as HTMLElement).getByPlaceholderText('warehouse'),
      'warehouse'
    )
    await actor.type(
      within(modal as HTMLElement).getByPlaceholderText('forgeql_app'),
      'forgeql_app'
    )
    await actor.type(
      within(modal as HTMLElement).getByPlaceholderText('Enter password'),
      'secret123'
    )
    await actor.click(
      within(modal as HTMLElement).getByRole('button', { name: 'Create datasource' })
    )

    expect(await screen.findByText('New warehouse was added successfully.')).toBeInTheDocument()
    expect(await screen.findByText('New warehouse')).toBeInTheDocument()
    expect(postedBodies[0]).toEqual({
      displayName: 'New warehouse',
      dbType: 'POSTGRESQL',
      host: 'db.internal.example',
      port: 5432,
      databaseName: 'warehouse',
      schemaName: 'public',
      username: 'forgeql_app',
      password: 'secret123',
      sslMode: 'PREFER',
      connectTimeoutMs: null,
      socketTimeoutMs: null,
      applicationName: null,
      sslRootCertRef: null,
      extraJdbcOptionsJson: null,
    })
  })

  it('shows the certificate field only for SSL verification modes', async () => {
    const fetchMock = installFetchMock()

    fetchMock.route(
      'GET',
      '/api/account/me',
      createJsonResponse(buildCurrentUser({ globalRole: 'MAIN_ADMIN' }))
    )
    fetchMock.route('GET', '/api/admin/users', createJsonResponse(adminUsersResponse))
    fetchMock.route('GET', '/api/datasource', createJsonResponse([]))

    renderAppAt('/datasource')

    const actor = userEvent.setup()
    await actor.click(await screen.findByRole('button', { name: 'Add datasource' }))

    const modalLabel = await screen.findByText('Datasource setup')
    const modal = modalLabel.closest('.surface-panel')
    expect(modal).not.toBeNull()

    expect(
      within(modal as HTMLElement).queryByText('SSL root cert ref')
    ).not.toBeInTheDocument()

    await actor.click(within(modal as HTMLElement).getByLabelText('SSL mode'))
    await actor.click(await screen.findByRole('option', { name: 'VERIFY_CA' }))

    expect(
      await within(modal as HTMLElement).findByText('SSL root cert ref')
    ).toBeInTheDocument()

    await actor.click(within(modal as HTMLElement).getByLabelText('SSL mode'))
    await actor.click(await screen.findByRole('option', { name: 'DISABLE' }))

    expect(
      within(modal as HTMLElement).queryByText('SSL root cert ref')
    ).not.toBeInTheDocument()
  })

  it('hides datasource add, update and delete actions for non-main-admin users', async () => {
    const fetchMock = installFetchMock()
    const datasource = buildDatasource({
      id: 16,
      displayName: 'Warehouse',
    })

    fetchMock.route('GET', '/api/account/me', createJsonResponse(buildCurrentUser()))
    fetchMock.route('GET', '/api/datasource', createJsonResponse([datasource]))

    renderAppAt('/datasource')

    expect(await screen.findByText('Warehouse')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Add datasource' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Update datasource' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Delete datasource' })).not.toBeInTheDocument()
  })

  it('keeps the main admin out of the user management table', async () => {
    const fetchMock = installFetchMock()

    fetchMock.route(
      'GET',
      '/api/account/me',
      createJsonResponse(buildCurrentUser({ globalRole: 'MAIN_ADMIN' }))
    )
    fetchMock.route('GET', '/api/admin/users', createJsonResponse(adminUsersResponse))
    fetchMock.route('GET', '/api/datasource', createJsonResponse([buildDatasource()]))

    renderAppAt('/datasource')

    expect(await screen.findByText('User management')).toBeInTheDocument()
    expect(await screen.findByText('member1')).toBeInTheDocument()
    expect(screen.queryByText('forgeql-owner')).not.toBeInTheDocument()
  })

  it('does not show success feedback when datasource refresh fails', async () => {
    const fetchMock = installFetchMock()
    let datasourceCalls = 0

    fetchMock.route('GET', '/api/account/me', createJsonResponse(buildCurrentUser()))
    fetchMock.route('GET', '/api/datasource', () => {
      datasourceCalls += 1
      return datasourceCalls === 1
        ? createJsonResponse([buildDatasource({ id: 17, displayName: 'Warehouse' })])
        : createApiErrorResponse(
            500,
            'DATABASE_OPERATION_FAILED',
            'Could not load datasources.',
            '/api/datasource'
          )
    })

    renderAppAt('/datasource')

    const actor = userEvent.setup()
    await screen.findByText('Warehouse')
    await actor.click(screen.getByRole('button', { name: 'Refresh datasources' }))

    expect(await screen.findByText('Could not load datasources.')).toBeInTheDocument()
    expect(screen.queryByText('Datasources refreshed.')).not.toBeInTheDocument()
  })
})
