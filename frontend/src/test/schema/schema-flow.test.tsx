import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { createApiErrorResponse, createJsonResponse, installFetchMock } from '../utils/fetchMock'
import {
  buildCurrentUser,
  buildDatasource,
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
})
