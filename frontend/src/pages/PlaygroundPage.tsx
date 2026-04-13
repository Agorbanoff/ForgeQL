import { startTransition, useDeferredValue, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { logOutUser } from '../api/accountApi'
import { getDataSource } from '../api/dataSourceApi'
import {
  aggregateRows,
  createRow,
  deleteRow,
  generateSchema,
  getSchema,
  readRows,
  refreshSchema,
  updateRow,
} from '../api/runtimeApi'
import { AnimatedSelect } from '../components/AnimatedSelect'
import { useElegantAnimations } from '../hooks/useElegantAnimations'
import { clearSavedDatasource, clearSessionActive, getStoredDatasourceDetails } from '../lib/appState'
import { schema as legacySchema } from '../data/schema'
import {
  AGGREGATE_OPTIONS,
  FILTER_OPERATOR_OPTIONS,
  formatCapabilityLabel,
  formatCellValue,
  formatDateTime,
  getAllowedAggregates,
  getAllowedOperators,
  getColumnByName,
  getColumnCategory,
  getPrimaryKeyColumn,
  getReadableColumns,
  getRequiredColumns,
  getTableTypeLabel,
  getWritableColumns,
  hasSingleNumericPrimaryKey,
  isValueRequired,
  parseDraftValue,
  serializeValueForInput,
  supportsListValue,
  supportsSecondValue,
} from '../lib/platform'
import type {
  AggregateFunction,
  AggregateResponse,
  DatasourceRecord,
  FilterOperator,
  GeneratedSchema,
  RowsResponse,
  SchemaTable,
  SortDirection,
} from '../types/platform'

type FeedbackTone = 'success' | 'danger' | 'warning' | 'neutral'
type Feedback = { tone: FeedbackTone; title: string; message: string }
type FilterDraft = { id: number; field: string; operator: FilterOperator; value: string; secondaryValue: string }
type AggregateDraft = { id: number; fn: AggregateFunction; field: string; alias: string }

const PAGE_SIZE_OPTIONS = ['10', '25', '50', '100'] as const
const SORT_DIRECTIONS = [
  { value: 'ASC', label: 'Ascending' },
  { value: 'DESC', label: 'Descending' },
] as const

const LOCAL_PREVIEW_ROWS: Record<string, Array<Record<string, unknown>>> = {
  users: [
    { id: 1, username: 'Aura', email: 'aura@house.dev', age: 24 },
    { id: 2, username: 'Kolev', email: 'kolev@house.dev', age: 27 },
    { id: 3, username: 'Mira', email: 'mira@house.dev', age: 31 },
    { id: 4, username: 'Dogan', email: 'dogan@house.dev', age: 29 },
  ],
  posts: [
    { id: 101, user_id: 1, title: 'Welcome Home', created_at: '2026-03-10', likes: 18 },
    { id: 102, user_id: 2, title: 'Memory Lane', created_at: '2026-03-12', likes: 42 },
    { id: 103, user_id: 2, title: 'Summer Room Tour', created_at: '2026-03-16', likes: 26 },
    { id: 104, user_id: 4, title: 'House Of Memories Launch', created_at: '2026-03-20', likes: 54 },
  ],
}

function feedbackClass(tone: FeedbackTone) {
  switch (tone) {
    case 'success':
      return 'border-emerald-400/20 bg-emerald-400/8 text-emerald-100'
    case 'danger':
      return 'border-red-500/20 bg-red-500/8 text-red-100'
    case 'warning':
      return 'border-amber-400/20 bg-amber-400/8 text-amber-100'
    default:
      return 'border-white/10 bg-white/[0.04] text-zinc-100'
  }
}

function safeString(value: unknown) {
  return value == null ? '' : String(value)
}

function nextId() {
  return Date.now() + Math.round(Math.random() * 1000)
}

function buildLocalPreviewSchema(): GeneratedSchema {
  const tables = Object.fromEntries(
    Object.entries(legacySchema.entities).map(([entityName, entity]) => {
      const sampleRow = LOCAL_PREVIEW_ROWS[entityName]?.[0] ?? {}
      const columns = entity.fields.map((field, index) => {
        const sampleValue = sampleRow[field]
        const isNumber = typeof sampleValue === 'number'
        const isDateLike =
          typeof sampleValue === 'string' &&
          /^\d{4}-\d{2}-\d{2}/.test(sampleValue)

        return [
          field,
          {
            name: field,
            dbType: isNumber ? 'integer' : isDateLike ? 'timestamp' : 'text',
            javaType: isNumber ? 'Long' : 'String',
            postgresTypeSchema: 'pg_catalog',
            postgresTypeName: isNumber ? 'int8' : isDateLike ? 'timestamp' : 'text',
            arrayElementTypeSchema: null,
            arrayElementTypeName: null,
            nullable: false,
            identity: field === entity.primaryKey,
            generated: false,
            defaultValue: null,
            position: index + 1,
            capabilities: {
              writable: field !== entity.primaryKey,
              filterable: true,
              sortable: true,
              aggregatable: isNumber,
            },
            precision: null,
            scale: null,
            length: null,
            enumType: false,
            enumLabels: [],
            uuidType: false,
            jsonType: false,
            jsonbType: false,
            arrayType: false,
            timestampWithoutTimeZone: isDateLike,
            timestampWithTimeZone: false,
            numericType: isNumber,
          },
        ]
      })

      return [
        entity.table,
        {
          name: entity.table,
          schema: 'public',
          qualifiedName: entity.table,
          tableType: 'TABLE',
          primaryKey: { columns: [entity.primaryKey] },
          uniqueConstraints: [],
          foreignKeys: [],
          relations: Object.entries(entity.relations ?? {}).map(([relationName, relation]) => ({
            name: relationName,
            relationType:
              relation.type === 'one-to-many'
                ? 'ONE_TO_MANY'
                : relation.type === 'many-to-one'
                  ? 'MANY_TO_ONE'
                  : 'ONE_TO_ONE',
            sourceQualifiedName: entity.table,
            targetQualifiedName: relation.target,
            sourceColumns: [relation.localKey],
            targetColumns: [relation.foreignKey],
          })),
          columns: columns.map(([, column]) => column),
          capabilities: {
            read: true,
            aggregate: true,
            insert: true,
            update: true,
            delete: true,
          },
        },
      ]
    })
  ) as GeneratedSchema['tables']

  return {
    datasourceId: 0,
    serverVersion: 'Local preview',
    generatedAt: new Date().toISOString(),
    defaultSchema: 'public',
    fingerprint: 'local-preview',
    tables,
    relationGraph: {
      users: ['posts'],
      posts: ['users'],
    },
  }
}

function buildEmptyDraft(table: SchemaTable | null) {
  if (!table) {
    return {}
  }

  return Object.fromEntries(getWritableColumns(table).map((column) => [column.name, '']))
}

function buildDraftFromRow(table: SchemaTable, row: Record<string, unknown>) {
  return Object.fromEntries(
    getWritableColumns(table).map((column) => [
      column.name,
      serializeValueForInput(column, row[column.name]),
    ])
  )
}

function buildFilterPayload(filters: FilterDraft[], table: SchemaTable) {
  return filters.reduce<Record<string, Record<string, unknown>>>((acc, filter) => {
    const column = getColumnByName(table, filter.field)
    if (!column) return acc
    if (!isValueRequired(filter.operator)) {
      acc[filter.field] = { ...(acc[filter.field] ?? {}), [filter.operator]: true }
      return acc
    }
    if (!filter.value.trim()) return acc
    const firstValue = parseDraftValue(column, filter.value)
    const payload =
      supportsListValue(filter.operator)
        ? filter.value.split(',').map((item) => parseDraftValue(column, item.trim()))
        : supportsSecondValue(filter.operator)
          ? [firstValue, parseDraftValue(column, filter.secondaryValue)]
          : firstValue
    acc[filter.field] = { ...(acc[filter.field] ?? {}), [filter.operator]: payload }
    return acc
  }, {})
}

function buildMutationValues(
  table: SchemaTable,
  draft: Record<string, string>,
  mode: 'create' | 'update',
  sourceRow?: Record<string, unknown> | null
) {
  const values: Record<string, unknown> = {}

  for (const column of getWritableColumns(table)) {
    const raw = draft[column.name] ?? ''
    const required = getRequiredColumns(table).some((item) => item.name === column.name)

    if (!raw.trim()) {
      if (mode === 'create' && required) {
        throw new Error(`${column.name} is required.`)
      }
      if (mode === 'create' && !required) {
        continue
      }
      if (mode === 'update' && column.nullable && sourceRow?.[column.name] != null) {
        values[column.name] = null
      }
      continue
    }

    const parsed = parseDraftValue(column, raw)
    if (mode === 'update' && sourceRow && sourceRow[column.name] === parsed) {
      continue
    }
    values[column.name] = parsed
  }

  if (mode === 'update' && Object.keys(values).length === 0) {
    throw new Error('Change at least one writable field before updating the row.')
  }

  return values
}

function applyLocalFilters(
  rows: Array<Record<string, unknown>>,
  filters: Record<string, Record<string, unknown>>
) {
  return rows.filter((row) =>
    Object.entries(filters).every(([field, operations]) =>
      Object.entries(operations).every(([operator, expected]) => {
        const actual = row[field]
        switch (operator) {
          case 'eq':
            return actual === expected
          case 'ne':
            return actual !== expected
          case 'gt':
            return String(actual ?? '') > String(expected ?? '')
          case 'gte':
            return String(actual ?? '') >= String(expected ?? '')
          case 'lt':
            return String(actual ?? '') < String(expected ?? '')
          case 'lte':
            return String(actual ?? '') <= String(expected ?? '')
          case 'like':
          case 'ilike':
            return String(actual ?? '')
              .toLowerCase()
              .includes(String(expected ?? '').toLowerCase())
          case 'in':
            return Array.isArray(expected) && expected.includes(actual)
          case 'between':
            return (
              Array.isArray(expected) &&
              expected.length === 2 &&
              String(actual ?? '') >= String(expected[0] ?? '') &&
              String(actual ?? '') <= String(expected[1] ?? '')
            )
          case 'isNull':
            return actual == null
          case 'isNotNull':
            return actual != null
          default:
            return true
        }
      })
    )
  )
}

export default function PlaygroundPage() {
  const navigate = useNavigate()
  const selection = getStoredDatasourceDetails()
  const rootRef = useElegantAnimations<HTMLDivElement>([])
  const [datasource, setDatasource] = useState<DatasourceRecord | null>(null)
  const [schema, setSchema] = useState<GeneratedSchema | null>(null)
  const [schemaLoading, setSchemaLoading] = useState(true)
  const [schemaRefreshing, setSchemaRefreshing] = useState(false)
  const [feedback, setFeedback] = useState<Feedback | null>(null)
  const [selectedTableName, setSelectedTableName] = useState<string | null>(null)
  const [tableSearch, setTableSearch] = useState('')
  const deferredTableSearch = useDeferredValue(tableSearch)
  const [visibleColumns, setVisibleColumns] = useState<string[]>([])
  const [filters, setFilters] = useState<FilterDraft[]>([])
  const [limit, setLimit] = useState('25')
  const [offset, setOffset] = useState(0)
  const [sortField, setSortField] = useState('')
  const [sortDirection, setSortDirection] = useState<SortDirection>('ASC')
  const [rowsState, setRowsState] = useState<RowsResponse | null>(null)
  const [rowsLoading, setRowsLoading] = useState(false)
  const [rowsError, setRowsError] = useState<string | null>(null)
  const [selectedRowIndex, setSelectedRowIndex] = useState<number | null>(null)
  const [createDraft, setCreateDraft] = useState<Record<string, string>>({})
  const [updateDraft, setUpdateDraft] = useState<Record<string, string>>({})
  const [aggregateSelections, setAggregateSelections] = useState<AggregateDraft[]>([
    { id: nextId(), fn: 'count', field: '', alias: 'row_count' },
  ])
  const [aggregateFilters, setAggregateFilters] = useState<FilterDraft[]>([])
  const [aggregateResult, setAggregateResult] = useState<AggregateResponse | null>(null)
  const [aggregateLoading, setAggregateLoading] = useState(false)
  const [aggregateError, setAggregateError] = useState<string | null>(null)
  const [mutating, setMutating] = useState(false)
  const [pendingDelete, setPendingDelete] = useState<Record<string, unknown> | null>(null)
  const [localPreviewMode, setLocalPreviewMode] = useState(false)
  const [localRowsByTable, setLocalRowsByTable] =
    useState<Record<string, Array<Record<string, unknown>>>>(LOCAL_PREVIEW_ROWS)

  const tables = useMemo(() => {
    const collection = Object.values(schema?.tables ?? {})
    const query = deferredTableSearch.trim().toLowerCase()
    if (!query) return collection
    return collection.filter((table) =>
      `${table.qualifiedName} ${table.tableType}`.toLowerCase().includes(query)
    )
  }, [deferredTableSearch, schema?.tables])

  const currentTable = useMemo(
    () => (selectedTableName ? schema?.tables[selectedTableName] ?? null : null),
    [schema?.tables, selectedTableName]
  )

  const currentRows = rowsState?.rows ?? []
  const selectedRow =
    selectedRowIndex == null ? null : (currentRows[selectedRowIndex] ?? null)

  const primaryKeyColumn = currentTable ? getPrimaryKeyColumn(currentTable) : null
  const mutationSurfaceEnabled = Boolean(
    currentTable?.tableType === 'TABLE' &&
      currentTable.primaryKey &&
      currentTable.primaryKey.columns.length === 1
  )
  const canCreate = Boolean(currentTable?.capabilities.insert && mutationSurfaceEnabled)
  const canEditRows = Boolean(
    currentTable?.capabilities.update &&
      currentTable &&
      mutationSurfaceEnabled &&
      hasSingleNumericPrimaryKey(currentTable)
  )
  const canDeleteRows = Boolean(
    currentTable?.capabilities.delete &&
      currentTable &&
      mutationSurfaceEnabled &&
      hasSingleNumericPrimaryKey(currentTable)
  )

  useEffect(() => {
    if (!selection) {
      navigate('/connection-request', { replace: true })
      return
    }
    void loadWorkspace(selection.id)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    if (!currentTable) return
    setVisibleColumns(getReadableColumns(currentTable).map((column) => column.name))
    setFilters([])
    setAggregateFilters([])
    setSortField('')
    setSortDirection('ASC')
    setOffset(0)
    setSelectedRowIndex(null)
    setCreateDraft(buildEmptyDraft(currentTable))
    setUpdateDraft(buildEmptyDraft(currentTable))
    setAggregateSelections([{ id: nextId(), fn: 'count', field: '', alias: 'row_count' }])
    void runReadQuery(
      currentTable,
      0,
      getReadableColumns(currentTable).map((column) => column.name),
      [],
      '',
      'ASC'
    )
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentTable?.qualifiedName])

  useEffect(() => {
    if (currentTable && selectedRow) {
      setUpdateDraft(buildDraftFromRow(currentTable, selectedRow))
    }
  }, [currentTable, selectedRow])

  async function loadWorkspace(datasourceId: number) {
    try {
      setSchemaLoading(true)
      const [nextDatasource, nextSchema] = await Promise.all([
        getDataSource(datasourceId),
        getSchema(datasourceId),
      ])
      setLocalPreviewMode(false)
      setDatasource(nextDatasource)
      setSchema(nextSchema)
      const firstTableName = Object.keys(nextSchema.tables)[0] ?? null
      startTransition(() => setSelectedTableName(firstTableName))
    } catch (error) {
      setLocalPreviewMode(true)
      setDatasource(
        selection
          ? {
              id: selection.id,
              ownerUserId: 0,
              displayName: selection.displayName,
              dbType: selection.dbType,
              host: selection.host,
              port: selection.port,
              databaseName: selection.databaseName,
              schemaName: selection.schemaName,
              username: selection.username,
              sslMode: 'PREFER',
              connectTimeoutMs: null,
              socketTimeoutMs: null,
              applicationName: null,
              sslRootCertRef: null,
              extraJdbcOptionsJson: null,
              status: 'ACTIVE',
              lastConnectionTestAt: null,
              lastConnectionStatus: 'UNTESTED',
              lastConnectionError: null,
              lastSchemaGeneratedAt: new Date().toISOString(),
              lastSchemaFingerprint: 'local-preview',
              serverVersion: 'Local preview',
              createdAt: new Date().toISOString(),
              updatedAt: new Date().toISOString(),
            }
          : null
      )
      const previewSchema = buildLocalPreviewSchema()
      setSchema(previewSchema)
      startTransition(() => setSelectedTableName(Object.keys(previewSchema.tables)[0] ?? null))
      setFeedback({
        tone: 'warning',
        title: 'Running in local preview mode',
        message:
          error instanceof Error
            ? `${error.message} The explorer is using local preview data so you can keep working.`
            : 'The backend workspace is unavailable, so the explorer is using local preview data.',
      })
    } finally {
      setSchemaLoading(false)
    }
  }

  async function runReadQuery(
    table = currentTable,
    nextOffset = offset,
    columns = visibleColumns,
    nextFilters = filters,
    nextSortField = sortField,
    nextSortDirection = sortDirection
  ) {
    if (!selection || !table) return
    try {
      setRowsLoading(true)
      setRowsError(null)
      if (localPreviewMode) {
        const filterPayload = buildFilterPayload(nextFilters, table)
        let rows = [...(localRowsByTable[table.name] ?? [])]
        rows = applyLocalFilters(rows, filterPayload)
        if (nextSortField) {
          rows.sort((left, right) => {
            const a = left[nextSortField]
            const b = right[nextSortField]
            const comparison = String(a ?? '').localeCompare(String(b ?? ''), undefined, {
              numeric: true,
            })
            return nextSortDirection === 'ASC' ? comparison : -comparison
          })
        }
        const pagedRows = rows.slice(nextOffset, nextOffset + Number(limit))
        setRowsState({
          rows: pagedRows.map((row) =>
            Object.fromEntries(columns.map((column) => [column, row[column]]))
          ),
          page: {
            returnedCount: pagedRows.length,
            limit: Number(limit),
            offset: nextOffset,
          },
        })
        return
      }

      const result = await readRows(selection.id, table.qualifiedName, {
        columns,
        filter: buildFilterPayload(nextFilters, table),
        sort: nextSortField
          ? [{ field: nextSortField, direction: nextSortDirection }]
          : undefined,
        limit: Number(limit),
        offset: nextOffset,
      })
      setRowsState(result)
    } catch (error) {
      setRowsError(error instanceof Error ? error.message : 'Could not load rows.')
    } finally {
      setRowsLoading(false)
    }
  }

  async function syncSchema(mode: 'generate' | 'refresh') {
    if (!selection) return
    if (localPreviewMode) {
      setFeedback({
        tone: 'warning',
        title: 'Local preview mode',
        message: 'Schema sync is unavailable until the backend datasource session is working again.',
      })
      return
    }
    try {
      setSchemaRefreshing(true)
      const nextSchema =
        mode === 'generate'
          ? await generateSchema(selection.id)
          : await refreshSchema(selection.id)
      setSchema(nextSchema)
      await loadWorkspace(selection.id)
      setFeedback({
        tone: 'success',
        title: mode === 'generate' ? 'Schema generated' : 'Schema refreshed',
        message: `${Object.keys(nextSchema.tables).length} runtime surfaces are ready.`,
      })
    } catch (error) {
      setFeedback({
        tone: 'danger',
        title: mode === 'generate' ? 'Schema generation failed' : 'Schema refresh failed',
        message: error instanceof Error ? error.message : 'Schema action failed.',
      })
    } finally {
      setSchemaRefreshing(false)
    }
  }

  async function runAggregate() {
    if (!selection || !currentTable) return
    try {
      setAggregateLoading(true)
      setAggregateError(null)
      const selections = aggregateSelections.map((selectionItem) => {
        const column = selectionItem.field
          ? getColumnByName(currentTable, selectionItem.field)
          : null
        if (selectionItem.fn !== 'count' && !column) {
          throw new Error(`Select a target column for ${selectionItem.fn}.`)
        }
        if (column && !getAllowedAggregates(column).includes(selectionItem.fn)) {
          throw new Error(`${selectionItem.fn} is not compatible with ${column.name}.`)
        }
        return {
          function: selectionItem.fn,
          field: selectionItem.field || undefined,
          alias: selectionItem.alias.trim() || undefined,
        }
      })
      if (localPreviewMode) {
        const filteredRows = applyLocalFilters(
          [...(localRowsByTable[currentTable.name] ?? [])],
          buildFilterPayload(effectiveAggregateFilters, currentTable)
        )
        const values = Object.fromEntries(
          selections.map((selection) => {
            const alias = selection.alias ?? selection.field ?? selection.function
            const numericValues = selection.field
              ? filteredRows
                  .map((row) => row[selection.field!])
                  .filter((value) => typeof value === 'number') as number[]
              : []

            switch (selection.function) {
              case 'count':
                return [alias, filteredRows.length]
              case 'sum':
                return [alias, numericValues.reduce((sum, value) => sum + value, 0)]
              case 'avg':
                return [
                  alias,
                  numericValues.length
                    ? numericValues.reduce((sum, value) => sum + value, 0) / numericValues.length
                    : null,
                ]
              case 'min':
                return [alias, numericValues.length ? Math.min(...numericValues) : null]
              case 'max':
                return [alias, numericValues.length ? Math.max(...numericValues) : null]
            }
          })
        )
        setAggregateResult({ rows: [{ values }] })
      } else {
        const result = await aggregateRows(selection.id, currentTable.qualifiedName, {
          selections,
          filter: buildFilterPayload(effectiveAggregateFilters, currentTable),
        })
        setAggregateResult(result)
      }
    } catch (error) {
      setAggregateError(error instanceof Error ? error.message : 'Aggregate query failed.')
    } finally {
      setAggregateLoading(false)
    }
  }

  async function submitCreate() {
    if (!selection || !currentTable) return
    try {
      setMutating(true)
      const values = buildMutationValues(currentTable, createDraft, 'create')
      let affectedRows = 1
      if (localPreviewMode) {
        const nextPrimaryKey =
          Math.max(
            0,
            ...(localRowsByTable[currentTable.name] ?? []).map((row) =>
              Number(row[primaryKeyColumn?.name ?? 'id']) || 0
            )
          ) + 1
        const nextRow = {
          ...(primaryKeyColumn ? { [primaryKeyColumn.name]: nextPrimaryKey } : {}),
          ...values,
        }
        setLocalRowsByTable((current) => ({
          ...current,
          [currentTable.name]: [...(current[currentTable.name] ?? []), nextRow],
        }))
      } else {
        const response = await createRow(selection.id, currentTable.qualifiedName, {
          values,
        })
        affectedRows = response.affectedRows
      }
      setFeedback({
        tone: 'success',
        title: 'Row created',
        message: `Inserted ${affectedRows} row${affectedRows === 1 ? '' : 's'} successfully.`,
      })
      setCreateDraft(buildEmptyDraft(currentTable))
      await runReadQuery()
    } catch (error) {
      setFeedback({
        tone: 'danger',
        title: 'Create failed',
        message: error instanceof Error ? error.message : 'Could not create the row.',
      })
    } finally {
      setMutating(false)
    }
  }

  async function submitUpdate() {
    if (!selection || !currentTable || !selectedRow || !primaryKeyColumn) return
    try {
      setMutating(true)
      const primaryKeyValue = Number(selectedRow[primaryKeyColumn.name])
      if (Number.isNaN(primaryKeyValue)) throw new Error('The selected row does not expose a numeric primary key.')
      const values = buildMutationValues(currentTable, updateDraft, 'update', selectedRow)
      if (localPreviewMode) {
        setLocalRowsByTable((current) => ({
          ...current,
          [currentTable.name]: (current[currentTable.name] ?? []).map((row) =>
            Number(row[primaryKeyColumn.name]) === primaryKeyValue
              ? { ...row, ...values }
              : row
          ),
        }))
      } else {
        await updateRow(selection.id, currentTable.qualifiedName, primaryKeyValue, {
          values,
        })
      }
      setFeedback({
        tone: 'success',
        title: 'Row updated',
        message: `Updated the record identified by ${primaryKeyColumn.name}=${primaryKeyValue}.`,
      })
      await runReadQuery()
    } catch (error) {
      setFeedback({
        tone: 'danger',
        title: 'Update failed',
        message: error instanceof Error ? error.message : 'Could not update the row.',
      })
    } finally {
      setMutating(false)
    }
  }

  async function submitDelete() {
    if (!selection || !currentTable || !pendingDelete || !primaryKeyColumn) return
    try {
      setMutating(true)
      const primaryKeyValue = Number(pendingDelete[primaryKeyColumn.name])
      if (Number.isNaN(primaryKeyValue)) throw new Error('The selected row does not expose a numeric primary key.')
      if (localPreviewMode) {
        setLocalRowsByTable((current) => ({
          ...current,
          [currentTable.name]: (current[currentTable.name] ?? []).filter(
            (row) => Number(row[primaryKeyColumn.name]) !== primaryKeyValue
          ),
        }))
      } else {
        await deleteRow(selection.id, currentTable.qualifiedName, primaryKeyValue)
      }
      setFeedback({
        tone: 'success',
        title: 'Row deleted',
        message: `Deleted the record identified by ${primaryKeyColumn.name}=${primaryKeyValue}.`,
      })
      setPendingDelete(null)
      setSelectedRowIndex(null)
      await runReadQuery()
    } catch (error) {
      setFeedback({
        tone: 'danger',
        title: 'Delete failed',
        message: error instanceof Error ? error.message : 'Could not delete the row.',
      })
    } finally {
      setMutating(false)
    }
  }

  async function handleLogOut() {
    try {
      await logOutUser()
    } catch {
      // Keep the local cleanup path even if the backend session already expired.
    } finally {
      clearSavedDatasource()
      clearSessionActive()
      navigate('/login', { replace: true })
    }
  }

  const visibleTableColumns = currentTable
    ? getReadableColumns(currentTable).filter((column) => visibleColumns.includes(column.name))
    : []
  const filterableColumns = currentTable?.columns.filter((column) => column.capabilities.filterable) ?? []
  const effectiveAggregateFilters = aggregateFilters.length > 0 ? aggregateFilters : filters

  return (
    <main ref={rootRef} className="page-shell py-6 sm:py-8" data-animate="scene">
      <div className="workspace-toolbar">
        <div className="flex flex-wrap items-center justify-end gap-3 pointer-events-auto">
          <button type="button" className="secondary-button" onClick={() => navigate('/connection-request')} data-pressable>
            Datasources
          </button>
          <button type="button" className="secondary-button" onClick={() => syncSchema(schema ? 'refresh' : 'generate')} disabled={schemaRefreshing} data-pressable>
            {schemaRefreshing ? 'Syncing...' : schema ? 'Refresh schema' : 'Generate schema'}
          </button>
          <button type="button" className="workspace-menu-trigger" onClick={handleLogOut} data-pressable>
            <span>
              <span className="workspace-menu-label">Session</span>
              <span className="workspace-menu-value">Log out</span>
            </span>
            <span className="workspace-menu-caret" />
          </button>
        </div>
      </div>

      <section className="surface-panel px-6 py-7 sm:px-8 sm:py-8 lg:px-10" data-animate="hero" data-reveal="right">
        <div className="relative z-10">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <span className="section-badge" data-animate="chip">Runtime explorer</span>
              <h1 className="display-title mt-6 text-[3rem] text-white sm:text-[4rem]">Schema-aware exploration with row control.</h1>
              <p className="display-copy mt-4 max-w-3xl text-sm sm:text-base">
                Inspect live tables, navigate relations, compose aggregate requests, and perform safe row-level mutations without free-form SQL.
              </p>
            </div>
            <div className="grid gap-3 sm:grid-cols-2">
              <div className="rounded-[24px] border border-white/8 bg-white/[0.03] px-4 py-4">
                <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">Datasource</p>
                <p className="mt-3 text-lg font-semibold text-white">{datasource?.displayName ?? selection?.displayName ?? 'Loading...'}</p>
                <p className="text-sm text-zinc-400">{datasource ? `${datasource.databaseName} / ${datasource.schemaName}` : 'Waiting for metadata'}</p>
              </div>
              <div className="rounded-[24px] border border-white/8 bg-white/[0.03] px-4 py-4">
                <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">Schema fingerprint</p>
                <p className="mt-3 text-sm font-semibold text-white">{schema?.fingerprint ?? datasource?.lastSchemaFingerprint ?? 'Not generated yet'}</p>
                <p className="text-sm text-zinc-400">{schema?.generatedAt ? formatDateTime(schema.generatedAt) : 'Generate the schema to begin.'}</p>
              </div>
            </div>
          </div>

          {feedback && (
            <div className={`mt-6 rounded-[24px] border p-4 ${feedbackClass(feedback.tone)}`} data-reveal="up">
              <p className="text-sm font-semibold">{feedback.title}</p>
              <p className="mt-2 text-sm opacity-90">{feedback.message}</p>
            </div>
          )}
        </div>
      </section>

      {schemaLoading ? (
        <section className="surface-panel mt-6 px-6 py-7 text-sm text-zinc-300" data-animate="panel">
          Loading datasource metadata and schema snapshot...
        </section>
      ) : !schema ? (
        <section className="surface-panel mt-6 px-6 py-7" data-animate="panel" data-reveal="up">
          <h2 className="text-2xl font-semibold text-white">No schema snapshot yet</h2>
          <p className="mt-3 max-w-2xl text-sm leading-7 text-zinc-300">
            Generate the schema snapshot first so the console can adapt to the live PostgreSQL structure at runtime.
          </p>
          <div className="mt-6 flex flex-wrap gap-3">
            <button type="button" className="primary-button" onClick={() => syncSchema('generate')} disabled={schemaRefreshing} data-pressable>
              {schemaRefreshing ? 'Generating...' : 'Generate schema now'}
            </button>
            <button type="button" className="secondary-button" onClick={() => navigate('/connection-request')} data-pressable>
              Back to datasource management
            </button>
          </div>
        </section>
      ) : (
        <div className="mt-6 grid gap-6 xl:grid-cols-[310px_minmax(0,1fr)_360px]">
          <aside className="space-y-6">
            <section className="surface-panel px-5 py-6 sm:px-6" data-animate="panel" data-reveal="right">
              <div className="flex items-end justify-between gap-3">
                <div>
                  <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">Schema explorer</p>
                  <h2 className="mt-3 text-2xl font-semibold text-white">Tables and views</h2>
                </div>
                <span className="small-chip">{tables.length} surfaces</span>
              </div>
              <input value={tableSearch} onChange={(event) => setTableSearch(event.target.value)} placeholder="Search table, view, schema..." className="input-shell mt-5" />
              <div className="mt-4 space-y-3 max-h-[62vh] overflow-auto pr-1">
                {tables.map((table) => (
                  <button
                    key={table.qualifiedName}
                    type="button"
                    className={`surface-card w-full p-4 text-left ${selectedTableName === table.qualifiedName ? 'ring-1 ring-cyan-300/30' : ''}`}
                    onClick={() => startTransition(() => setSelectedTableName(table.qualifiedName))}
                    data-pressable
                    data-reveal="right"
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <p className="text-sm font-semibold text-white">{table.name}</p>
                        <p className="mt-1 text-xs uppercase tracking-[0.22em] text-zinc-500">{getTableTypeLabel(table.tableType)}</p>
                      </div>
                      <span className="small-chip">{table.columns.length} cols</span>
                    </div>
                    <div className="mt-3 flex flex-wrap gap-2">
                      {['read', 'aggregate', 'insert', 'update', 'delete'].map((capability) => (
                        <span key={capability} className={`mini-capability ${table.capabilities[capability as keyof typeof table.capabilities] ? 'is-active' : ''}`}>
                          {capability}
                        </span>
                      ))}
                    </div>
                  </button>
                ))}
              </div>
            </section>

            {currentTable && (
              <section className="surface-panel px-5 py-6 sm:px-6" data-animate="panel" data-reveal="up">
                <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">Table detail</p>
                <h2 className="mt-3 text-2xl font-semibold text-white">{currentTable.qualifiedName}</h2>
                <p className="mt-3 text-sm text-zinc-300">{formatCapabilityLabel(currentTable)}</p>
                <div className="mt-5 space-y-3">
                  <DetailRow label="Primary key" value={currentTable.primaryKey?.columns.join(', ') ?? 'None'} />
                  <DetailRow label="Unique constraints" value={currentTable.uniqueConstraints.length ? currentTable.uniqueConstraints.map((item) => item.columns.join(', ')).join(' | ') : 'None'} />
                  <DetailRow label="Relations" value={currentTable.relations.length ? currentTable.relations.map((item) => `${item.relationType} -> ${item.targetQualifiedName}`).join(' | ') : 'None'} />
                </div>
              </section>
            )}
          </aside>

          <section className="space-y-6">
            {currentTable && (
              <>
                <section className="surface-panel surface-overflow-visible px-5 py-6 sm:px-6" data-animate="panel" data-reveal="right">
                  <div className="flex flex-wrap items-end justify-between gap-4">
                    <div>
                      <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">Table explorer</p>
                      <h2 className="mt-3 text-2xl font-semibold text-white">{currentTable.name}</h2>
                    </div>
                    <div className="flex flex-wrap gap-3">
                      <button type="button" className="secondary-button" onClick={() => { setOffset(0); void runReadQuery(currentTable, 0) }} disabled={rowsLoading} data-pressable>
                        {rowsLoading ? 'Loading...' : 'Run data query'}
                      </button>
                      <span className="small-chip">{getTableTypeLabel(currentTable.tableType)}</span>
                    </div>
                  </div>

                  <div className="mt-5 grid gap-4 lg:grid-cols-[minmax(0,1fr)_180px_180px]">
                    <div className="surface-card surface-overflow-visible p-4">
                      <p className="text-xs uppercase tracking-[0.2em] text-zinc-500">Visible columns</p>
                      <div className="mt-4 flex flex-wrap gap-2">
                        {getReadableColumns(currentTable).map((column) => {
                          const active = visibleColumns.includes(column.name)
                          return (
                            <button key={column.name} type="button" className={`chip-toggle ${active ? 'is-active' : ''}`} onClick={() => setVisibleColumns((current) => active ? current.filter((item) => item !== column.name) : [...current, column.name])}>
                              {column.name}
                            </button>
                          )
                        })}
                      </div>
                    </div>

                    <div className="surface-card surface-overflow-visible p-4">
                      <p className="text-xs uppercase tracking-[0.2em] text-zinc-500">Sort field</p>
                      <div className="mt-3">
                        <AnimatedSelect value={sortField} onChange={(value) => setSortField(value)} options={getReadableColumns(currentTable).filter((column) => column.capabilities.sortable).map((column) => ({ value: column.name, label: column.name }))} placeholder="No sort" ariaLabel="Sort field" />
                      </div>
                      <div className="mt-3">
                        <AnimatedSelect value={sortDirection} onChange={(value) => setSortDirection(value as SortDirection)} options={SORT_DIRECTIONS} ariaLabel="Sort direction" disabled={!sortField} />
                      </div>
                    </div>

                    <div className="surface-card surface-overflow-visible p-4">
                      <p className="text-xs uppercase tracking-[0.2em] text-zinc-500">Pagination</p>
                      <div className="mt-3">
                        <AnimatedSelect value={limit} onChange={(value) => setLimit(value)} options={PAGE_SIZE_OPTIONS.map((value) => ({ value, label: `${value} rows` }))} ariaLabel="Page size" />
                      </div>
                      <div className="mt-3 flex gap-2">
                        <button type="button" className="secondary-button px-4 py-2" onClick={() => { const next = Math.max(0, offset - Number(limit)); setOffset(next); void runReadQuery(currentTable, next) }} disabled={rowsLoading || offset === 0} data-pressable>Prev</button>
                        <button type="button" className="secondary-button px-4 py-2" onClick={() => { const next = offset + Number(limit); setOffset(next); void runReadQuery(currentTable, next) }} disabled={rowsLoading || (rowsState?.page.returnedCount ?? 0) < Number(limit)} data-pressable>Next</button>
                      </div>
                    </div>
                  </div>

                  <div className="mt-5 surface-card surface-overflow-visible p-4">
                    <div className="flex items-center justify-between gap-3">
                      <p className="text-xs uppercase tracking-[0.2em] text-zinc-500">Filters</p>
                      <button type="button" className="secondary-button px-4 py-2" onClick={() => setFilters((current) => [...current, { id: nextId(), field: filterableColumns[0]?.name ?? '', operator: filterableColumns[0] ? getAllowedOperators(filterableColumns[0])[0] : 'eq', value: '', secondaryValue: '' }])} disabled={filterableColumns.length === 0} data-pressable>Add filter</button>
                    </div>
                    <div className="mt-4 space-y-3">
                      {filters.length === 0 && <div className="rounded-[18px] border border-white/8 bg-white/[0.03] p-4 text-sm text-zinc-400">No active filters yet.</div>}
                      {filters.map((filter) => (
                        <FilterRow
                          key={filter.id}
                          table={currentTable}
                          value={filter}
                          onChange={(next) => setFilters((current) => current.map((item) => item.id === filter.id ? next : item))}
                          onRemove={() => setFilters((current) => current.filter((item) => item.id !== filter.id))}
                        />
                      ))}
                    </div>
                  </div>

                  <div className="mt-5 overflow-hidden rounded-[24px] border border-white/8">
                    {rowsError && <div className="border-b border-red-500/20 bg-red-500/8 px-4 py-3 text-sm text-red-100">{rowsError}</div>}
                    <div className="overflow-auto">
                      <table className="data-table w-full min-w-[760px]">
                        <thead>
                          <tr>
                            {visibleTableColumns.map((column) => <th key={column.name}>{column.name}</th>)}
                          </tr>
                        </thead>
                        <tbody>
                          {!rowsLoading && currentRows.length === 0 && (
                            <tr><td colSpan={Math.max(visibleTableColumns.length, 1)} className="text-center text-zinc-400">No rows match the current request.</td></tr>
                          )}
                          {currentRows.map((row, index) => (
                            <tr key={`${index}-${safeString(row[primaryKeyColumn?.name ?? index])}`} className={selectedRowIndex === index ? 'is-selected' : ''} onClick={() => setSelectedRowIndex(index)}>
                              {visibleTableColumns.map((column) => <td key={column.name}>{formatCellValue(row[column.name])}</td>)}
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                </section>

                <section className="surface-panel surface-overflow-visible px-5 py-6 sm:px-6" data-animate="panel" data-reveal="left">
                  <div className="flex flex-wrap items-end justify-between gap-4">
                    <div>
                      <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">Request blueprint</p>
                      <h2 className="mt-3 text-2xl font-semibold text-white">Controlled query builder</h2>
                    </div>
                    <div className="small-chip">REST mapped, SQL hidden</div>
                  </div>
                  <pre className="data-block mt-5">{JSON.stringify({ table: currentTable.qualifiedName, columns: visibleColumns, filter: buildFilterPayload(filters, currentTable), sort: sortField ? [{ field: sortField, direction: sortDirection }] : [], limit: Number(limit), offset, aggregates: aggregateSelections.map((item) => ({ function: item.fn, field: item.field || undefined, alias: item.alias || undefined })), aggregateFilter: buildFilterPayload(effectiveAggregateFilters, currentTable) }, null, 2)}</pre>
                </section>
              </>
            )}
          </section>

          <aside className="space-y-6">
            {currentTable && (
              <>
                <section className="surface-panel surface-overflow-visible px-5 py-6 sm:px-6" data-animate="panel" data-reveal="left">
                  <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">Aggregate studio</p>
                  <h2 className="mt-3 text-2xl font-semibold text-white">Counts and rollups</h2>
                  <div className="mt-5 space-y-3">
                    <div className="surface-card surface-overflow-visible p-4">
                      <div className="flex items-center justify-between gap-3">
                        <p className="text-xs uppercase tracking-[0.2em] text-zinc-500">Aggregate filters</p>
                        <button type="button" className="secondary-button px-4 py-2" onClick={() => setAggregateFilters((current) => [...current, { id: nextId(), field: filterableColumns[0]?.name ?? '', operator: filterableColumns[0] ? getAllowedOperators(filterableColumns[0])[0] : 'eq', value: '', secondaryValue: '' }])} disabled={filterableColumns.length === 0} data-pressable>Add filter</button>
                      </div>
                      <p className="mt-3 text-sm text-zinc-400">
                        If you leave this section empty, the aggregate query inherits the grid filters from the table explorer.
                      </p>
                      <div className="mt-4 space-y-3">
                        {aggregateFilters.map((filter) => (
                          <FilterRow
                            key={filter.id}
                            table={currentTable}
                            value={filter}
                            onChange={(next) => setAggregateFilters((current) => current.map((item) => item.id === filter.id ? next : item))}
                            onRemove={() => setAggregateFilters((current) => current.filter((item) => item.id !== filter.id))}
                          />
                        ))}
                      </div>
                    </div>
                    {aggregateSelections.map((selectionItem) => (
                      <div key={selectionItem.id} className="surface-card surface-overflow-visible p-4">
                        <div className="grid gap-3">
                          <AnimatedSelect value={selectionItem.fn} onChange={(value) => setAggregateSelections((current) => current.map((item) => item.id === selectionItem.id ? { ...item, fn: value as AggregateFunction, field: value === 'count' ? item.field : item.field } : item))} options={AGGREGATE_OPTIONS} ariaLabel="Aggregate function" />
                          <AnimatedSelect value={selectionItem.field} onChange={(value) => setAggregateSelections((current) => current.map((item) => item.id === selectionItem.id ? { ...item, field: value } : item))} options={getReadableColumns(currentTable).filter((column) => getAllowedAggregates(column).includes(selectionItem.fn)).map((column) => ({ value: column.name, label: column.name }))} placeholder={selectionItem.fn === 'count' ? 'Optional target column' : 'Select target column'} ariaLabel="Aggregate target" />
                          <input value={selectionItem.alias} onChange={(event) => setAggregateSelections((current) => current.map((item) => item.id === selectionItem.id ? { ...item, alias: event.target.value } : item))} placeholder="Alias" className="input-shell" />
                        </div>
                      </div>
                    ))}
                    <div className="flex flex-wrap gap-3">
                      <button type="button" className="secondary-button" onClick={() => setAggregateSelections((current) => [...current, { id: nextId(), fn: 'count', field: '', alias: '' }])} data-pressable>Add aggregate</button>
                      <button type="button" className="primary-button" onClick={runAggregate} disabled={aggregateLoading} data-pressable>{aggregateLoading ? 'Running...' : 'Run aggregate'}</button>
                    </div>
                    {aggregateError && <div className="rounded-[18px] border border-red-500/20 bg-red-500/8 p-4 text-sm text-red-100">{aggregateError}</div>}
                    {aggregateResult?.rows?.[0] && (
                      <div className="grid gap-3">
                        {Object.entries(aggregateResult.rows[0].values).map(([key, value]) => (
                          <div key={key} className="surface-card p-4">
                            <p className="text-xs uppercase tracking-[0.2em] text-zinc-500">{key}</p>
                            <p className="mt-2 text-2xl font-semibold text-white">{formatCellValue(value)}</p>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </section>

                <section className="surface-panel surface-overflow-visible px-5 py-6 sm:px-6" data-animate="panel" data-reveal="left">
                  <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">Mutation studio</p>
                  <h2 className="mt-3 text-2xl font-semibold text-white">Create, update, delete</h2>
                  <p className="mt-3 text-sm text-zinc-400">
                    Mutations are enabled only for writable tables with a single primary key.
                  </p>
                  <div className="mt-5 space-y-5">
                    <MutationFields title="Create row" table={currentTable} draft={createDraft} onChange={setCreateDraft} disabled={!canCreate || mutating} emptyLabel="Create is disabled for this schema surface." />
                    <button type="button" className="primary-button w-full" onClick={submitCreate} disabled={!canCreate || mutating} data-pressable>{mutating ? 'Saving...' : 'Create row'}</button>
                    <div className="subtle-divider" />
                    <MutationFields title="Update selected row" table={currentTable} draft={updateDraft} onChange={setUpdateDraft} disabled={!canEditRows || !selectedRow || mutating} emptyLabel="Select a row in the grid to edit it." />
                    <button type="button" className="secondary-button w-full" onClick={submitUpdate} disabled={!canEditRows || !selectedRow || mutating} data-pressable>Update selected row</button>
                    <button type="button" className="secondary-button w-full text-red-100" onClick={() => setPendingDelete(selectedRow)} disabled={!canDeleteRows || !selectedRow || mutating} data-pressable>Delete selected row</button>
                  </div>
                </section>
              </>
            )}
          </aside>
        </div>
      )}

      {pendingDelete && primaryKeyColumn && (
        <div className="fixed inset-0 z-[120] flex items-center justify-center bg-slate-950/70 px-4 backdrop-blur-md">
          <div className="surface-panel w-full max-w-md px-6 py-6" data-reveal="up">
            <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">Confirm delete</p>
            <h2 className="mt-3 text-2xl font-semibold text-white">Delete row {safeString(pendingDelete[primaryKeyColumn.name])}?</h2>
            <p className="mt-4 text-sm leading-6 text-zinc-300">This action removes a single row identified by the primary key. Bulk deletes are intentionally not available.</p>
            <div className="mt-6 flex flex-wrap justify-end gap-3">
              <button type="button" className="secondary-button" onClick={() => setPendingDelete(null)} data-pressable>Cancel</button>
              <button type="button" className="primary-button" onClick={submitDelete} disabled={mutating} data-pressable>{mutating ? 'Deleting...' : 'Delete row'}</button>
            </div>
          </div>
        </div>
      )}
    </main>
  )
}

function DetailRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-[20px] border border-white/8 bg-white/[0.03] p-4">
      <p className="text-xs uppercase tracking-[0.2em] text-zinc-500">{label}</p>
      <p className="mt-2 text-sm text-zinc-100">{value}</p>
    </div>
  )
}

function FilterRow({
  table,
  value,
  onChange,
  onRemove,
}: {
  table: SchemaTable
  value: FilterDraft
  onChange: (next: FilterDraft) => void
  onRemove: () => void
}) {
  const filterableColumns = table.columns.filter((item) => item.capabilities.filterable)
  const column = getColumnByName(table, value.field) ?? filterableColumns[0]
  if (!column) {
    return (
      <div className="rounded-[20px] border border-white/8 bg-white/[0.03] p-4 text-sm text-zinc-400">
        No filterable columns are exposed for this surface.
      </div>
    )
  }
  const operators = getAllowedOperators(column)
  const operator = operators.includes(value.operator) ? value.operator : operators[0]
  return (
    <div className="grid gap-3 rounded-[20px] border border-white/8 bg-white/[0.03] p-4">
      <AnimatedSelect value={value.field} onChange={(field) => onChange({ ...value, field, operator: getAllowedOperators(getColumnByName(table, field) ?? column)[0] ?? 'eq' })} options={filterableColumns.map((item) => ({ value: item.name, label: item.name, description: getColumnCategory(item) }))} ariaLabel="Filter field" />
      <AnimatedSelect value={operator} onChange={(next) => onChange({ ...value, operator: next as FilterOperator })} options={FILTER_OPERATOR_OPTIONS.filter((item) => operators.includes(item.value))} ariaLabel="Filter operator" />
      {isValueRequired(operator) && <input value={value.value} onChange={(event) => onChange({ ...value, value: event.target.value })} placeholder={supportsListValue(operator) ? 'Comma-separated values' : 'Value'} className="input-shell" />}
      {supportsSecondValue(operator) && <input value={value.secondaryValue} onChange={(event) => onChange({ ...value, secondaryValue: event.target.value })} placeholder="Second value" className="input-shell" />}
      <button type="button" className="secondary-button px-4 py-2" onClick={onRemove} data-pressable>Remove</button>
    </div>
  )
}

function MutationFields({
  title,
  table,
  draft,
  onChange,
  disabled,
  emptyLabel,
}: {
  title: string
  table: SchemaTable
  draft: Record<string, string>
  onChange: (next: Record<string, string>) => void
  disabled: boolean
  emptyLabel: string
}) {
  const writableColumns = getWritableColumns(table)
  return (
    <div>
      <p className="text-sm font-semibold text-white">{title}</p>
      <div className="mt-3 space-y-3">
        {writableColumns.length === 0 && <div className="rounded-[18px] border border-white/8 bg-white/[0.03] p-4 text-sm text-zinc-400">{emptyLabel}</div>}
        {writableColumns.map((column) => {
          const category = getColumnCategory(column)
          const required = getRequiredColumns(table).some((item) => item.name === column.name)
          return (
            <div key={column.name} className="space-y-2">
              <label className="text-sm font-medium text-zinc-300">{column.name}{required ? ' *' : ''}</label>
              {category === 'enum' ? (
                <AnimatedSelect value={draft[column.name] ?? ''} onChange={(value) => onChange({ ...draft, [column.name]: value })} options={column.enumLabels.map((value) => ({ value, label: value }))} placeholder="Select a value" ariaLabel={column.name} disabled={disabled} />
              ) : category === 'boolean' ? (
                <AnimatedSelect value={draft[column.name] ?? ''} onChange={(value) => onChange({ ...draft, [column.name]: value })} options={[{ value: 'true', label: 'true' }, { value: 'false', label: 'false' }]} placeholder="Choose true or false" ariaLabel={column.name} disabled={disabled} />
              ) : category === 'json' || category === 'array' ? (
                <textarea value={draft[column.name] ?? ''} onChange={(event) => onChange({ ...draft, [column.name]: event.target.value })} className="input-shell min-h-[110px] resize-y" disabled={disabled} />
              ) : (
                <input value={draft[column.name] ?? ''} onChange={(event) => onChange({ ...draft, [column.name]: event.target.value })} type={category === 'number' ? 'number' : category === 'date' ? 'date' : category === 'time' ? 'time' : category === 'datetime' ? 'datetime-local' : 'text'} className="input-shell" disabled={disabled} />
              )}
            </div>
          )
        })}
      </div>
    </div>
  )
}
