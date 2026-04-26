import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { createApiErrorResponse, createJsonResponse, installFetchMock } from '../utils/fetchMock'
import {
  buildCurrentUser,
  buildDatasource,
  buildGeneratedSchema,
  buildRowsResponse,
  buildSchemaSummary,
  buildTable,
} from '../utils/fixtures'
import { renderAppAt } from '../utils/render'

function registerExplorerBootstrap(
  datasourceId: number,
  tables = [buildTable({ name: 'orders', qualifiedName: 'public.orders' })]
) {
  const fetchMock = installFetchMock()
  const datasource = buildDatasource({ id: datasourceId })

  fetchMock.route('GET', '/api/account/me', createJsonResponse(buildCurrentUser()))
  fetchMock.route('GET', `/api/datasource/${datasourceId}`, createJsonResponse(datasource))
  fetchMock.route(
    'GET',
    `/api/core/datasources/${datasourceId}/schema/summary`,
    createJsonResponse(buildSchemaSummary({ datasourceId, tableCount: tables.length }))
  )
  fetchMock.route(
    'GET',
    `/api/core/datasources/${datasourceId}/tables`,
    createJsonResponse({ tables })
  )
  fetchMock.route(
    'GET',
    (request) =>
      request.url.pathname ===
      `/api/core/datasources/${datasourceId}/tables/public.orders/rows`,
    createJsonResponse(buildRowsResponse([{ id: 1, name: 'Alpha', amount: 10 }]))
  )

  return fetchMock
}

describe('schema flow', () => {
  it('shows an empty schema state when no generated schema exists yet', async () => {
    const fetchMock = installFetchMock()

    fetchMock.route('GET', '/api/account/me', createJsonResponse(buildCurrentUser()))
    fetchMock.route('GET', '/api/datasource/1', createJsonResponse(buildDatasource({ id: 1 })))
    fetchMock.route(
      'GET',
      '/api/core/datasources/1/schema/summary',
      createApiErrorResponse(
        404,
        'GENERATED_SCHEMA_NOT_FOUND',
        'No generated schema exists for this datasource yet.',
        '/api/core/datasources/1/schema/summary'
      )
    )
    fetchMock.route('GET', '/api/core/datasources/1/tables', createJsonResponse({ tables: [] }))

    renderAppAt('/datasource/1/explorer')

    expect(await screen.findByText('No schema snapshot yet')).toBeInTheDocument()
    expect(
      screen.getByText(/Generate the schema snapshot first/i)
    ).toBeInTheDocument()
  })

  it('renders the table list returned by the runtime schema endpoints', async () => {
    registerExplorerBootstrap(1, [
      buildTable({ name: 'orders', qualifiedName: 'public.orders' }),
      buildTable({ name: 'customers', qualifiedName: 'public.customers' }),
    ])

    renderAppAt('/datasource/1/explorer')

    expect(await screen.findByText('Tables and views')).toBeInTheDocument()
    expect(screen.getByText('orders')).toBeInTheDocument()
    expect(screen.getByText('customers')).toBeInTheDocument()
  })

  it('lets a viewer generate schema and continue browsing data', async () => {
    const fetchMock = installFetchMock()
    const viewer = buildCurrentUser({ username: 'viewer1', globalRole: 'VIEWER' })
    const datasource = buildDatasource({ id: 2, accessRole: 'VIEWER' })
    const tables = [buildTable({ name: 'orders', qualifiedName: 'public.orders' })]
    let summaryCalls = 0
    let tableCalls = 0

    fetchMock.route('GET', '/api/account/me', createJsonResponse(viewer))
    fetchMock.route('GET', '/api/datasource/2', createJsonResponse(datasource))
    fetchMock.route('GET', '/api/core/datasources/2/schema/summary', () => {
      summaryCalls += 1
      return summaryCalls === 1
        ? createApiErrorResponse(
            404,
            'GENERATED_SCHEMA_NOT_FOUND',
            'No generated schema exists for this datasource yet.',
            '/api/core/datasources/2/schema/summary'
          )
        : createJsonResponse(buildSchemaSummary({ datasourceId: 2, tableCount: tables.length }))
    })
    fetchMock.route('GET', '/api/core/datasources/2/tables', () => {
      tableCalls += 1
      return createJsonResponse({ tables: tableCalls === 1 ? [] : tables })
    })
    fetchMock.route(
      'POST',
      '/api/core/datasources/2/schema/generate',
      createJsonResponse({
        schema: buildGeneratedSchema(tables, 2),
      })
    )
    fetchMock.route(
      'GET',
      (request) =>
        request.url.pathname === '/api/core/datasources/2/tables/public.orders/rows',
      createJsonResponse(buildRowsResponse([{ id: 1, name: 'Alpha', amount: 10 }]))
    )

    renderAppAt('/datasource/2/explorer')

    const actor = userEvent.setup()
    await actor.click(await screen.findByRole('button', { name: 'Generate schema now' }))

    expect(await screen.findByText('Tables and views')).toBeInTheDocument()
    expect(screen.getByText('orders')).toBeInTheDocument()
  })

  it('hides row mutation controls for viewers even when schema capabilities include writes', async () => {
    const fetchMock = installFetchMock()
    const viewer = buildCurrentUser({ username: 'viewer1', globalRole: 'VIEWER' })
    const datasource = buildDatasource({ id: 3, accessRole: 'VIEWER' })

    fetchMock.route('GET', '/api/account/me', createJsonResponse(viewer))
    fetchMock.route('GET', '/api/datasource/3', createJsonResponse(datasource))
    fetchMock.route(
      'GET',
      '/api/core/datasources/3/schema/summary',
      createJsonResponse(buildSchemaSummary({ datasourceId: 3, tableCount: 1 }))
    )
    fetchMock.route(
      'GET',
      '/api/core/datasources/3/tables',
      createJsonResponse({
        tables: [buildTable({ name: 'orders', qualifiedName: 'public.orders' })],
      })
    )
    fetchMock.route(
      'GET',
      (request) =>
        request.url.pathname === '/api/core/datasources/3/tables/public.orders/rows',
      createJsonResponse(buildRowsResponse([{ id: 1, name: 'Alpha', amount: 10 }]))
    )

    renderAppAt('/datasource/3/explorer')

    expect(await screen.findByText('Tables and views')).toBeInTheDocument()
    expect(screen.queryByText('Create, update, delete')).not.toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Create row' })
    ).not.toBeInTheDocument()
  })
})
