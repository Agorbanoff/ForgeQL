import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { createApiErrorResponse, createJsonResponse, installFetchMock } from '../utils/fetchMock'
import { buildCurrentUser, buildDatasource, buildGeneratedSchema } from '../utils/fixtures'
import { renderAppAt } from '../utils/render'

describe('datasource flow', () => {
  it('renders datasource records returned by the backend', async () => {
    const fetchMock = installFetchMock()
    const datasource = buildDatasource({ id: 7, displayName: 'Warehouse', host: 'warehouse.db' })

    fetchMock.route('GET', '/api/account/me', createJsonResponse(buildCurrentUser()))
    fetchMock.route('GET', '/api/datasource', createJsonResponse([datasource]))

    renderAppAt('/datasource')

    expect(await screen.findByText('Warehouse')).toBeInTheDocument()
  })

  it('shows the empty datasource state when the backend returns no records', async () => {
    const fetchMock = installFetchMock()

    fetchMock.route('GET', '/api/account/me', createJsonResponse(buildCurrentUser()))
    fetchMock.route('GET', '/api/datasource', createJsonResponse([]))

    renderAppAt('/datasource')

    expect(await screen.findByText(/No datasources yet/i)).toBeInTheDocument()
  })

  it('shows the backend error when datasource loading fails', async () => {
    const fetchMock = installFetchMock()

    fetchMock.route('GET', '/api/account/me', createJsonResponse(buildCurrentUser()))
    fetchMock.route(
      'GET',
      '/api/datasource',
      createApiErrorResponse(500, 'DATABASE_OPERATION_FAILED', 'Could not load datasources.', '/api/datasource')
    )

    renderAppAt('/datasource')

    expect(await screen.findByText('Datasource catalog unavailable')).toBeInTheDocument()
    expect(screen.getByText('Could not load datasources.')).toBeInTheDocument()
  })

  it('submits datasource creation to the API and refreshes the list afterwards', async () => {
    const fetchMock = installFetchMock()
    const createdDatasource = buildDatasource({
      id: 11,
      displayName: 'New warehouse',
      host: 'db.internal.example',
      databaseName: 'warehouse',
      username: 'forgeql_app',
      lastConnectionStatus: 'UNTESTED',
      lastSchemaGeneratedAt: null,
    })
    const postedBodies: unknown[] = []
    let datasourceCalls = 0

    fetchMock.route('GET', '/api/account/me', createJsonResponse(buildCurrentUser()))
    fetchMock.route('GET', '/api/datasource', () => {
      datasourceCalls += 1
      return createJsonResponse(datasourceCalls === 1 ? [] : [createdDatasource])
    })
    fetchMock.route('POST', '/api/datasource', (request) => {
      postedBodies.push(request.bodyJson)
      return createJsonResponse({})
    })
    fetchMock.route(
      'POST',
      '/api/datasource/11/test-connection',
      createJsonResponse({
        datasourceId: 11,
        successful: false,
        status: 'ACTIVE',
        lastConnectionTestAt: '2026-04-13T12:10:00.000Z',
        lastConnectionStatus: 'FAILED',
        lastConnectionError: 'Connection refused',
        databaseProductName: 'PostgreSQL',
        serverVersion: null,
        message: 'Connection refused',
      })
    )

    renderAppAt('/datasource')

    const actor = userEvent.setup()
    await actor.type(
      await screen.findByPlaceholderText('Production analytics'),
      'New warehouse'
    )
    await actor.type(screen.getByPlaceholderText('db.internal.example'), 'db.internal.example')
    await actor.clear(screen.getByPlaceholderText('warehouse'))
    await actor.type(screen.getByPlaceholderText('warehouse'), 'warehouse')
    await actor.type(screen.getByPlaceholderText('forgeql_app'), 'forgeql_app')
    await actor.type(screen.getByPlaceholderText('Enter password'), 'secret123')
    await actor.click(screen.getByRole('button', { name: 'Create datasource' }))

    expect(await screen.findByText('Datasource saved')).toBeInTheDocument()
    expect(screen.getByText('Connection refused')).toBeInTheDocument()
    expect(
      await screen.findByRole('heading', { name: 'New warehouse', level: 2 })
    ).toBeInTheDocument()

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

  it('runs the connection test action and shows the backend result', async () => {
    const fetchMock = installFetchMock()
    const datasource = buildDatasource({
      id: 4,
      displayName: 'Warehouse',
      lastConnectionStatus: 'FAILED',
      lastConnectionError: 'Credentials expired',
    })
    let datasourceCalls = 0

    fetchMock.route('GET', '/api/account/me', createJsonResponse(buildCurrentUser()))
    fetchMock.route('GET', '/api/datasource', () => {
      datasourceCalls += 1
      return createJsonResponse(
        datasourceCalls === 1
          ? [datasource]
          : [
              {
                ...datasource,
                lastConnectionStatus: 'SUCCEEDED',
                lastConnectionError: null,
              },
            ]
      )
    })
    fetchMock.route(
      'POST',
      '/api/datasource/4/test-connection',
      createJsonResponse({
        datasourceId: 4,
        successful: true,
        status: 'ACTIVE',
        lastConnectionTestAt: '2026-04-13T12:20:00.000Z',
        lastConnectionStatus: 'SUCCEEDED',
        lastConnectionError: null,
        databaseProductName: 'PostgreSQL',
        serverVersion: '16.3',
        message: 'Connection succeeded',
      })
    )

    renderAppAt('/datasource')

    const actor = userEvent.setup()
    const warehouseCard = await screen.findByText('Warehouse')
    const card = warehouseCard.closest('article')

    expect(card).not.toBeNull()

    await actor.click(within(card as HTMLElement).getByRole('button', { name: 'Test' }))

    expect(await screen.findByText('Connection verified')).toBeInTheDocument()
    expect(screen.getByText('Connection succeeded')).toBeInTheDocument()
  })

  it('shows a loading state while schema generation is running', async () => {
    const fetchMock = installFetchMock()
    const datasource = buildDatasource({
      id: 5,
      displayName: 'Warehouse',
      lastConnectionStatus: 'SUCCEEDED',
      lastSchemaGeneratedAt: null,
    })

    let resolveSchema: ((value: Response) => void) | undefined
    const schemaPromise = new Promise<Response>((resolve) => {
      resolveSchema = resolve
    })

    fetchMock.route('GET', '/api/account/me', createJsonResponse(buildCurrentUser()))
    fetchMock.route('GET', '/api/datasource', createJsonResponse([datasource]))
    fetchMock.route('POST', '/api/core/datasources/5/schema/generate', () => schemaPromise)

    renderAppAt('/datasource')

    const actor = userEvent.setup()
    await actor.click(await screen.findByRole('button', { name: 'Generate schema' }))

    expect(await screen.findByRole('button', { name: 'Generating...' })).toBeInTheDocument()

    if (resolveSchema) {
      resolveSchema(
        createJsonResponse({
          schema: buildGeneratedSchema([]),
        })
      )
    }

    await waitFor(() => {
      expect(fetchMock.getCalls('POST', '/api/core/datasources/5/schema/generate')).toHaveLength(1)
    })
  })
})
