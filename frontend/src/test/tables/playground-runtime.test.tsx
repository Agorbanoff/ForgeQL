import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
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

function registerRuntimeExplorer(fetchMock: ReturnType<typeof installFetchMock>) {
  const datasource = buildDatasource({ id: 1 })
  const table = buildTable({ name: 'orders', qualifiedName: 'public.orders' })

  fetchMock.route('GET', '/api/account/me', createJsonResponse(buildCurrentUser()))
  fetchMock.route('GET', '/api/datasource/1', createJsonResponse(datasource))
  fetchMock.route(
    'GET',
    '/api/core/datasources/1/schema/summary',
    createJsonResponse(buildSchemaSummary())
  )
  fetchMock.route(
    'GET',
    '/api/core/datasources/1/tables',
    createJsonResponse({ tables: [table] })
  )

  return table
}

describe('runtime explorer', () => {
  it('fetches rows and requests the next page when pagination advances', async () => {
    const fetchMock = installFetchMock()
    registerRuntimeExplorer(fetchMock)

    fetchMock.route('GET', (request) => {
      if (
        request.url.pathname !== '/api/core/datasources/1/tables/public.orders/rows'
      ) {
        return false
      }

      return request.url.searchParams.get('offset') === '0'
    }, createJsonResponse(
      buildRowsResponse(
        Array.from({ length: 25 }, (_, index) => ({
          id: index + 1,
          name: `Order ${index + 1}`,
          amount: index + 1,
        })),
        25,
        0
      )
    ))
    fetchMock.route('GET', (request) => {
      if (
        request.url.pathname !== '/api/core/datasources/1/tables/public.orders/rows'
      ) {
        return false
      }

      return request.url.searchParams.get('offset') === '25'
    }, createJsonResponse(
      buildRowsResponse([{ id: 26, name: 'Order 26', amount: 26 }], 25, 25)
    ))

    renderAppAt('/datasource/1/explorer')

    expect(await screen.findByText('Order 1')).toBeInTheDocument()

    const actor = userEvent.setup()
    await actor.click(screen.getByRole('button', { name: 'Next' }))

    expect(await screen.findByText('Order 26')).toBeInTheDocument()

    const rowCalls = fetchMock.getCalls(
      'GET',
      (request) =>
        request.url.pathname === '/api/core/datasources/1/tables/public.orders/rows'
    )

    expect(rowCalls[0]?.path).toContain('limit=25')
    expect(rowCalls[0]?.path).toContain('offset=0')
    expect(rowCalls[1]?.path).toContain('offset=25')
  })

  it('shows backend row-loading errors in the explorer', async () => {
    const fetchMock = installFetchMock()
    registerRuntimeExplorer(fetchMock)

    fetchMock.route(
      'GET',
      (request) =>
        request.url.pathname === '/api/core/datasources/1/tables/public.orders/rows',
      createApiErrorResponse(
        500,
        'UNEXPECTED_POSTGRES_FAILURE',
        'Unexpected PostgreSQL failure',
        '/api/core/datasources/1/tables/public.orders/rows'
      )
    )

    renderAppAt('/datasource/1/explorer')

    expect(await screen.findByText('Could not load rows')).toBeInTheDocument()
    expect(screen.getByText('Unexpected PostgreSQL failure')).toBeInTheDocument()
    expect(screen.getByText(/Code: UNEXPECTED_POSTGRES_FAILURE/i)).toBeInTheDocument()
  })

  it('sends the aggregate request payload and renders the result', async () => {
    const fetchMock = installFetchMock()
    registerRuntimeExplorer(fetchMock)

    fetchMock.route(
      'GET',
      (request) =>
        request.url.pathname === '/api/core/datasources/1/tables/public.orders/rows',
      createJsonResponse(buildRowsResponse([{ id: 1, name: 'Alpha', amount: 10 }]))
    )

    let aggregateBody: unknown = null
    fetchMock.route(
      'POST',
      '/api/core/datasources/1/tables/public.orders/aggregate',
      (request) => {
        aggregateBody = request.bodyJson
        return createJsonResponse({
          rows: [
            {
              values: {
                row_count: 1,
              },
            },
          ],
        })
      }
    )

    renderAppAt('/datasource/1/explorer')

    const actor = userEvent.setup()
    await actor.click(await screen.findByRole('button', { name: 'Run aggregate' }))

    expect(await screen.findByText('row_count')).toBeInTheDocument()
    expect(aggregateBody).toEqual({
      selections: [
        {
          function: 'count',
          alias: 'row_count',
        },
      ],
    })
  })

  it('creates, updates, and deletes rows through the runtime API and refreshes the grid', async () => {
    const fetchMock = installFetchMock()
    registerRuntimeExplorer(fetchMock)

    let rows = [
      { id: 1, name: 'Alpha', amount: 10 },
      { id: 2, name: 'Beta', amount: 20 },
    ]

    fetchMock.route(
      'GET',
      (request) =>
        request.url.pathname === '/api/core/datasources/1/tables/public.orders/rows',
      () => createJsonResponse(buildRowsResponse(rows))
    )

    let createdBody: unknown = null
    fetchMock.route(
      'POST',
      '/api/core/datasources/1/tables/public.orders/rows',
      (request) => {
        createdBody = request.bodyJson
        rows = [...rows, { id: 3, name: 'Gamma', amount: 30 }]
        return createJsonResponse({
          affectedRows: 1,
          createdIdentity: 3,
          row: rows[2],
        })
      }
    )

    let updatedBody: unknown = null
    fetchMock.route(
      'PATCH',
      '/api/core/datasources/1/tables/public.orders/rows/3',
      (request) => {
        updatedBody = request.bodyJson
        rows = rows.map((row) =>
          row.id === 3 ? { ...row, name: 'Gamma Prime' } : row
        )
        return createJsonResponse({
          affectedRows: 1,
          row: rows.find((row) => row.id === 3) ?? null,
        })
      }
    )

    fetchMock.route(
      'DELETE',
      '/api/core/datasources/1/tables/public.orders/rows/3',
      () => {
        rows = rows.filter((row) => row.id !== 3)
        return createJsonResponse({
          affectedRows: 1,
          deletedIdentity: 3,
        })
      }
    )

    renderAppAt('/datasource/1/explorer')

    const actor = userEvent.setup()
    expect(await screen.findByText('Alpha')).toBeInTheDocument()

    const createCard = screen.getByText('Create row', { selector: 'p' }).parentElement?.parentElement
    expect(createCard).not.toBeNull()

    const createScope = within(createCard as HTMLElement)
    await actor.type(createScope.getByRole('textbox'), 'Gamma')
    await actor.clear(createScope.getByRole('spinbutton'))
    await actor.type(createScope.getByRole('spinbutton'), '30')
    await actor.click(createScope.getByRole('button', { name: 'Create row' }))

    expect(await screen.findByText('Row created')).toBeInTheDocument()
    expect(await screen.findByText('Gamma')).toBeInTheDocument()
    expect(createdBody).toEqual({
      values: {
        name: 'Gamma',
        amount: 30,
      },
    })

    await actor.click(screen.getByText('Gamma'))
    await actor.click(screen.getByRole('button', { name: /edit the selected row/i }))

    const updateCard = await screen.findByText('Update selected row', { selector: 'p' })
    const updateScope = within(updateCard.parentElement?.parentElement as HTMLElement)
    const updateTextbox = updateScope.getByRole('textbox')
    await actor.clear(updateTextbox)
    await actor.type(updateTextbox, 'Gamma Prime')
    await actor.click(updateScope.getByRole('button', { name: 'Update selected row' }))

    expect(await screen.findByText('Row updated')).toBeInTheDocument()
    expect(await screen.findByText('Gamma Prime')).toBeInTheDocument()
    expect(updatedBody).toEqual({
      values: {
        name: 'Gamma Prime',
      },
    })

    await actor.click(screen.getByText('Gamma Prime'))
    await actor.click(screen.getByRole('button', { name: /remove the selected row/i }))
    await actor.click(screen.getByRole('button', { name: 'Delete selected row' }))
    await actor.click(await screen.findByRole('button', { name: 'Delete row' }))

    expect(await screen.findByText('Row deleted')).toBeInTheDocument()
    await waitFor(() => {
      expect(screen.queryByText('Gamma Prime')).not.toBeInTheDocument()
    })
  })

  it('opens the session dropdown and deletes the account after confirmation', async () => {
    const fetchMock = installFetchMock()
    registerRuntimeExplorer(fetchMock)

    fetchMock.route(
      'GET',
      (request) =>
        request.url.pathname === '/api/core/datasources/1/tables/public.orders/rows',
      createJsonResponse(buildRowsResponse([{ id: 1, name: 'Alpha', amount: 10 }]))
    )
    fetchMock.route('DELETE', '/api/account/me', createJsonResponse({}))

    renderAppAt('/datasource/1/explorer')

    const actor = userEvent.setup()
    expect(await screen.findByText('Alpha')).toBeInTheDocument()

    await actor.click(screen.getByRole('button', { name: /alex/i }))
    await actor.click(await screen.findByRole('menuitem', { name: 'Delete account' }))

    expect(
      await screen.findByText('Are you sure you want to delete your account?')
    ).toBeInTheDocument()

    await actor.click(screen.getByRole('button', { name: 'Delete account' }))

    expect(await screen.findByText('Log in')).toBeInTheDocument()
    expect(fetchMock.getCalls('DELETE', '/api/account/me')).toHaveLength(1)
  })
})
