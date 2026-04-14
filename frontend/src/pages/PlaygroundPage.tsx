import {
  startTransition,
  useDeferredValue,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useAuth } from '../auth/AuthProvider'
import { deleteCurrentAccount } from '../api/accountApi'
import { getDataSource } from '../api/dataSourceApi'
import { ApiRequestError } from '../api/http'
import {
  aggregateRows,
  createRow,
  deleteRow,
  generateSchema,
  getSchemaSummary,
  listTables,
  readRows,
  refreshSchema,
  updateRow,
} from '../api/runtimeApi'
import { AnimatedSelect } from '../components/AnimatedSelect'
import { useElegantAnimations } from '../hooks/useElegantAnimations'
import {
  clearSavedDatasource,
  storeSelectedDatasource,
} from '../lib/appState'
import {
  AGGREGATE_OPTIONS,
  FILTER_OPERATOR_OPTIONS,
  formatCellValue,
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
  RowsResponse,
  SchemaSummary,
  SchemaTable,
  SortDirection,
} from '../types/platform'

type FeedbackTone = 'success' | 'danger' | 'warning' | 'neutral'
type Feedback = { tone: FeedbackTone; title: string; message: string }
type ExplorerError = { title: string; message: string; detail?: string }
type MutationMode = 'create' | 'update' | 'delete'
type FilterDraft = {
  id: number
  field: string
  operator: FilterOperator
  value: string
  secondaryValue: string
}
type AggregateDraft = { id: number; fn: AggregateFunction; field: string; alias: string }

const PAGE_SIZE_OPTIONS = ['10', '25', '50', '100'] as const
const SORT_DIRECTIONS = [
  { value: 'ASC', label: 'Ascending' },
  { value: 'DESC', label: 'Descending' },
] as const
const SCHEMA_FILTER_ALL = '__ALL__'
const SCHEMA_FILTER_SYSTEM = '__SYSTEM__'
const SYSTEM_SCHEMA_NAMES = new Set([
  'auth',
  'extensions',
  'graphql',
  'graphql_public',
  'information_schema',
  'pg_catalog',
  'pg_toast',
  'realtime',
  'storage',
  'supabase_functions',
  'supabase_migrations',
  'vault',
])
const TABLE_CAPABILITIES = [
  { key: 'read', label: 'Browse' },
  { key: 'aggregate', label: 'Aggregate' },
  { key: 'insert', label: 'Create' },
  { key: 'update', label: 'Update' },
  { key: 'delete', label: 'Delete' },
] as const

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

function isAuthError(error: unknown) {
  return error instanceof ApiRequestError && error.status === 401
}

function isSchemaMissingError(error: unknown) {
  return (
    error instanceof ApiRequestError &&
    (error.code === 'GENERATED_SCHEMA_NOT_FOUND' ||
      (error.status === 404 && error.targetPath?.includes('/schema')))
  )
}

function isDatasourceMissingError(error: unknown) {
  return error instanceof ApiRequestError && error.code === 'DATASOURCE_NOT_FOUND'
}

function toExplorerError(error: unknown, fallback: string): ExplorerError {
  if (error instanceof ApiRequestError) {
    const detailParts = [
      error.code ? `Code: ${error.code}` : null,
      error.status ? `Status: ${error.status}` : null,
      error.targetPath ? `Path: ${error.targetPath}` : null,
    ].filter(Boolean)

    return {
      title: error.code === 'VALIDATION_ERROR' ? 'Filter could not be applied' : 'Could not load rows',
      message: error.message || fallback,
      detail: detailParts.length > 0 ? detailParts.join(' • ') : undefined,
    }
  }

  if (error instanceof Error) {
    return {
      title: 'Could not load rows',
      message: error.message || fallback,
    }
  }

  return {
    title: 'Could not load rows',
    message: fallback,
  }
}

function toStoredSelection(datasource: DatasourceRecord) {
  return {
    id: datasource.id,
    displayName: datasource.displayName,
    dbType: datasource.dbType,
    host: datasource.host,
    port: datasource.port,
    databaseName: datasource.databaseName,
    schemaName: datasource.schemaName,
    username: datasource.username,
    lastSchemaGeneratedAt: datasource.lastSchemaGeneratedAt,
    lastSchemaFingerprint: datasource.lastSchemaFingerprint,
  }
}

function isSystemSchema(schemaName: string) {
  const normalized = schemaName.trim().toLowerCase()
  return normalized.startsWith('pg_') || SYSTEM_SCHEMA_NAMES.has(normalized)
}

function formatCount(count: number, singular: string, plural = `${singular}s`) {
  return `${count} ${count === 1 ? singular : plural}`
}

function compareSchemaNames(left: string, right: string, preferredSchema?: string | null) {
  const leftPreferred = preferredSchema != null && left === preferredSchema
  const rightPreferred = preferredSchema != null && right === preferredSchema

  if (leftPreferred && !rightPreferred) {
    return -1
  }

  if (!leftPreferred && rightPreferred) {
    return 1
  }

  const leftSystem = isSystemSchema(left)
  const rightSystem = isSystemSchema(right)

  if (leftSystem !== rightSystem) {
    return leftSystem ? 1 : -1
  }

  return left.localeCompare(right)
}

function pickPreferredSchema(
  tables: SchemaTable[],
  datasourceSchema?: string | null
) {
  if (!tables.length) {
    return null
  }

  if (datasourceSchema && tables.some((table) => table.schema === datasourceSchema)) {
    return datasourceSchema
  }

  return tables.find((table) => !isSystemSchema(table.schema))?.schema ?? tables[0].schema
}

function pickDefaultTable(
  tables: SchemaTable[],
  preferredSchema?: string | null,
  preferredTableName?: string | null
) {
  if (preferredTableName) {
    const matchingTable =
      tables.find((table) => table.qualifiedName === preferredTableName) ?? null

    if (matchingTable) {
      return matchingTable
    }
  }

  if (preferredSchema) {
    const schemaTable = tables.find((table) => table.schema === preferredSchema) ?? null

    if (schemaTable) {
      return schemaTable
    }
  }

  return tables.find((table) => !isSystemSchema(table.schema)) ?? tables[0] ?? null
}

function getEditableMutationColumns(table: SchemaTable) {
  return getWritableColumns(table).filter(
    (column) =>
      !column.identity &&
      !column.generated &&
      column.defaultValue == null
  )
}

function getActiveCapabilities(table: SchemaTable) {
  return TABLE_CAPABILITIES.filter((capability) => table.capabilities[capability.key])
}

function buildEmptyDraft(table: SchemaTable | null) {
  if (!table) {
    return {}
  }

  return Object.fromEntries(
    getEditableMutationColumns(table).map((column) => [column.name, ''])
  )
}

function buildDraftFromRow(table: SchemaTable, row: Record<string, unknown>) {
  return Object.fromEntries(
    getEditableMutationColumns(table).map((column) => [
      column.name,
      serializeValueForInput(column, row[column.name]),
    ])
  )
}

function buildFilterPayload(filters: FilterDraft[], table: SchemaTable) {
  return filters.reduce<Record<string, Record<string, unknown>>>((acc, filter) => {
    const column = getColumnByName(table, filter.field)

    if (!column) {
      return acc
    }

    if (!isValueRequired(filter.operator)) {
      acc[filter.field] = {
        ...(acc[filter.field] ?? {}),
        [filter.operator]: true,
      }
      return acc
    }

    if (!filter.value.trim()) {
      return acc
    }

    const firstValue = parseDraftValue(column, filter.value)
    const payload =
      supportsListValue(filter.operator)
        ? filter.value
            .split(',')
            .map((item) => parseDraftValue(column, item.trim()))
        : supportsSecondValue(filter.operator)
          ? [firstValue, parseDraftValue(column, filter.secondaryValue)]
          : firstValue

    acc[filter.field] = {
      ...(acc[filter.field] ?? {}),
      [filter.operator]: payload,
    }

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
  const editableColumns = getEditableMutationColumns(table)

  for (const column of editableColumns) {
    const raw = draft[column.name] ?? ''
    const required = getRequiredColumns(table).some(
      (item) => item.name === column.name
    )

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
    throw new Error('Change at least one editable field before updating the row.')
  }

  return values
}

export default function PlaygroundPage() {
  const navigate = useNavigate()
  const { datasourceId: datasourceIdParam } = useParams()
  const { logout, user } = useAuth()
  const rootRef = useElegantAnimations<HTMLDivElement>([])
  const workspaceMenuRef = useRef<HTMLDivElement | null>(null)
  const datasourceId = Number(datasourceIdParam)

  const [datasource, setDatasource] = useState<DatasourceRecord | null>(null)
  const [schemaSummary, setSchemaSummary] = useState<SchemaSummary | null>(null)
  const [tables, setTables] = useState<SchemaTable[]>([])
  const [workspaceLoading, setWorkspaceLoading] = useState(true)
  const [schemaSyncing, setSchemaSyncing] = useState(false)
  const [feedback, setFeedback] = useState<Feedback | null>(null)
  const [selectedTableName, setSelectedTableName] = useState<string | null>(null)
  const [schemaFilter, setSchemaFilter] = useState('')
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
  const [rowsError, setRowsError] = useState<ExplorerError | null>(null)
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
  const [accountDeleting, setAccountDeleting] = useState(false)
  const [workspaceMenuOpen, setWorkspaceMenuOpen] = useState(false)
  const [pendingAccountDelete, setPendingAccountDelete] = useState(false)
  const [pendingDelete, setPendingDelete] = useState<Record<string, unknown> | null>(
    null
  )
  const [mutationMode, setMutationMode] = useState<MutationMode>('create')

  const preferredSchema = useMemo(
    () => pickPreferredSchema(tables, datasource?.schemaName),
    [datasource?.schemaName, tables]
  )

  const schemaNames = useMemo(() => {
    const uniqueSchemas = [...new Set(tables.map((table) => table.schema))]
    return uniqueSchemas.sort((left, right) =>
      compareSchemaNames(left, right, preferredSchema)
    )
  }, [preferredSchema, tables])

  const systemSchemaNames = useMemo(
    () => schemaNames.filter((schemaName) => isSystemSchema(schemaName)),
    [schemaNames]
  )
  const nonSystemSchemaNames = useMemo(
    () => schemaNames.filter((schemaName) => !isSystemSchema(schemaName)),
    [schemaNames]
  )
  const systemTableCount = useMemo(
    () => tables.filter((table) => isSystemSchema(table.schema)).length,
    [tables]
  )

  useEffect(() => {
    if (!tables.length) {
      setSchemaFilter('')
      return
    }

    setSchemaFilter((current) => {
      if (current === SCHEMA_FILTER_ALL || current === SCHEMA_FILTER_SYSTEM) {
        return current
      }

      if (current && tables.some((table) => table.schema === current)) {
        return current
      }

      return preferredSchema ?? SCHEMA_FILTER_ALL
    })
  }, [preferredSchema, tables])

  const schemaFilterOptions = useMemo(() => {
    const options: Array<{ value: string; label: string; description?: string }> = []

    for (const schemaName of nonSystemSchemaNames) {
      const count = tables.filter((table) => table.schema === schemaName).length
      const isPreferred = schemaName === preferredSchema

      options.push({
        value: schemaName,
        label: schemaName,
        description: `${isPreferred ? 'Default focus' : 'User schema'} - ${formatCount(count, 'resource')}`,
      })
    }

    if (systemSchemaNames.length > 0) {
      options.push({
        value: SCHEMA_FILTER_SYSTEM,
        label: 'System schemas',
        description: `${formatCount(systemTableCount, 'resource')} across auth, storage, pg_* and related schemas`,
      })
    }

    if (schemaNames.length > 1) {
      options.push({
        value: SCHEMA_FILTER_ALL,
        label: 'All schemas',
        description: `${formatCount(tables.length, 'resource')} returned by the backend`,
      })
    }

    return options
  }, [
    nonSystemSchemaNames,
    preferredSchema,
    schemaNames.length,
    systemSchemaNames.length,
    systemTableCount,
    tables,
  ])

  const schemaScopedTables = useMemo(() => {
    if (schemaFilter === SCHEMA_FILTER_ALL) {
      return tables
    }

    if (schemaFilter === SCHEMA_FILTER_SYSTEM) {
      return tables.filter((table) => isSystemSchema(table.schema))
    }

    if (schemaFilter) {
      return tables.filter((table) => table.schema === schemaFilter)
    }

    return tables
  }, [schemaFilter, tables])

  const filteredTables = useMemo(() => {
    const query = deferredTableSearch.trim().toLowerCase()

    if (!query) {
      return schemaScopedTables
    }

    return schemaScopedTables.filter((table) =>
      `${table.schema} ${table.qualifiedName} ${table.tableType}`.toLowerCase().includes(query)
    )
  }, [deferredTableSearch, schemaScopedTables])

  const groupedFilteredTables = useMemo(() => {
    const grouped = new Map<string, SchemaTable[]>()

    for (const table of filteredTables) {
      grouped.set(table.schema, [...(grouped.get(table.schema) ?? []), table])
    }

    return [...grouped.entries()]
      .sort(([left], [right]) => compareSchemaNames(left, right, preferredSchema))
      .map(([schemaName, schemaTables]) => ({
        schemaName,
        tables: schemaTables.sort((left, right) => left.name.localeCompare(right.name)),
      }))
  }, [filteredTables, preferredSchema])

  useEffect(() => {
    if (!filteredTables.length) {
      return
    }

    if (!selectedTableName || !filteredTables.some((table) => table.qualifiedName === selectedTableName)) {
      startTransition(() => setSelectedTableName(filteredTables[0].qualifiedName))
    }
  }, [filteredTables, selectedTableName])

  const currentTable = useMemo(
    () => tables.find((table) => table.qualifiedName === selectedTableName) ?? null,
    [selectedTableName, tables]
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
  const mutationSupported = canCreate || canEditRows || canDeleteRows
  const mutationModeOptions = useMemo(
    () => [
      {
        value: 'create' as const,
        label: 'Create',
        description: 'Insert a new row',
        enabled: canCreate,
      },
      {
        value: 'update' as const,
        label: 'Update',
        description: 'Edit the selected row',
        enabled: canEditRows,
      },
      {
        value: 'delete' as const,
        label: 'Delete',
        description: 'Remove the selected row',
        enabled: canDeleteRows,
      },
    ],
    [canCreate, canDeleteRows, canEditRows]
  )

  const visibleTableColumns = currentTable
    ? getReadableColumns(currentTable).filter((column) =>
        visibleColumns.includes(column.name)
      )
    : []
  const filterableColumns =
    currentTable?.columns.filter((column) => column.capabilities.filterable) ?? []
  const effectiveAggregateFilters =
    aggregateFilters.length > 0 ? aggregateFilters : filters
  const activeCapabilities = currentTable ? getActiveCapabilities(currentTable) : []
  const editableMutationColumns = currentTable ? getEditableMutationColumns(currentTable) : []
  const hiddenSystemTables =
    systemTableCount > 0 &&
    schemaFilter !== SCHEMA_FILTER_ALL &&
    schemaFilter !== SCHEMA_FILTER_SYSTEM &&
    !isSystemSchema(schemaFilter || preferredSchema || '')
      ? systemTableCount
      : 0

  function resetTableState() {
    setRowsState(null)
    setRowsError(null)
    setSelectedRowIndex(null)
    setVisibleColumns([])
    setFilters([])
    setLimit('25')
    setOffset(0)
    setSortField('')
    setSortDirection('ASC')
    setCreateDraft({})
    setUpdateDraft({})
    setAggregateSelections([{ id: nextId(), fn: 'count', field: '', alias: 'row_count' }])
    setAggregateFilters([])
    setAggregateResult(null)
    setAggregateError(null)
    setPendingDelete(null)
  }

  async function handleAuthFailure() {
    clearSavedDatasource()
    await logout({ skipRequest: true })
    navigate('/login', { replace: true })
  }

  async function loadWorkspace(preferredTableName?: string | null) {
    if (!Number.isInteger(datasourceId) || datasourceId <= 0) {
      navigate('/datasource', { replace: true })
      return
    }

    try {
      setWorkspaceLoading(true)
      const nextDatasource = await getDataSource(datasourceId)
      setDatasource(nextDatasource)
      storeSelectedDatasource(toStoredSelection(nextDatasource))

      try {
        const [nextSchemaSummary, nextTables] = await Promise.all([
          getSchemaSummary(datasourceId),
          listTables(datasourceId),
        ])

        setSchemaSummary(nextSchemaSummary)
        setTables(nextTables)

        const candidateTable = pickDefaultTable(
          nextTables,
          pickPreferredSchema(nextTables, nextDatasource.schemaName),
          preferredTableName ?? selectedTableName
        )

        startTransition(() => {
          setSelectedTableName(candidateTable?.qualifiedName ?? null)
        })
      } catch (error) {
        if (isSchemaMissingError(error)) {
          setSchemaSummary(null)
          setTables([])
          startTransition(() => setSelectedTableName(null))
          resetTableState()
          return
        }

        throw error
      }
    } catch (error) {
      if (isAuthError(error)) {
        await handleAuthFailure()
        return
      }

      if (isDatasourceMissingError(error)) {
        clearSavedDatasource()
        navigate('/datasource', { replace: true })
        return
      }

      setFeedback({
        tone: 'danger',
        title: 'Workspace unavailable',
        message:
          error instanceof Error
            ? error.message
            : 'Could not load the datasource workspace.',
      })
    } finally {
      setWorkspaceLoading(false)
    }
  }

  useEffect(() => {
    void loadWorkspace()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [datasourceId])

  useEffect(() => {
    if (!currentTable) {
      resetTableState()
      return
    }

    const nextVisibleColumns = getReadableColumns(currentTable).map(
      (column) => column.name
    )

    setVisibleColumns(nextVisibleColumns)
    setFilters([])
    setAggregateFilters([])
    setSortField('')
    setSortDirection('ASC')
    setOffset(0)
    setSelectedRowIndex(null)
    setCreateDraft(buildEmptyDraft(currentTable))
    setUpdateDraft(buildEmptyDraft(currentTable))
    setAggregateSelections([{ id: nextId(), fn: 'count', field: '', alias: 'row_count' }])
    setAggregateResult(null)
    setAggregateError(null)

    void runReadQuery(currentTable, 0, nextVisibleColumns, [], '', 'ASC')
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentTable?.qualifiedName])

  useEffect(() => {
    if (currentTable && selectedRow) {
      setUpdateDraft(buildDraftFromRow(currentTable, selectedRow))
    }
  }, [currentTable, selectedRow])

  useEffect(() => {
    if (!mutationSupported) {
      return
    }

    if (!mutationModeOptions.some((option) => option.value === mutationMode && option.enabled)) {
      const fallback = mutationModeOptions.find((option) => option.enabled)
      if (fallback) {
        setMutationMode(fallback.value)
      }
    }
  }, [mutationMode, mutationModeOptions, mutationSupported])

  useEffect(() => {
    if (!workspaceMenuOpen) {
      return
    }

    function handlePointerDown(event: PointerEvent) {
      const target = event.target as Node
      if (workspaceMenuRef.current?.contains(target)) {
        return
      }

      setWorkspaceMenuOpen(false)
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setWorkspaceMenuOpen(false)
      }
    }

    window.addEventListener('pointerdown', handlePointerDown)
    window.addEventListener('keydown', handleKeyDown)

    return () => {
      window.removeEventListener('pointerdown', handlePointerDown)
      window.removeEventListener('keydown', handleKeyDown)
    }
  }, [workspaceMenuOpen])

  async function runReadQuery(
    table = currentTable,
    nextOffset = offset,
    columns = visibleColumns,
    nextFilters = filters,
    nextSortField = sortField,
    nextSortDirection = sortDirection
  ) {
    if (!table || !Number.isInteger(datasourceId) || datasourceId <= 0) {
      return
    }

    try {
      setRowsLoading(true)
      setRowsError(null)
      setSelectedRowIndex(null)

      const filterPayload = buildFilterPayload(nextFilters, table)
      const result = await readRows(datasourceId, table.qualifiedName, {
        columns,
        filter:
          Object.keys(filterPayload).length > 0 ? filterPayload : undefined,
        sort: nextSortField
          ? [{ field: nextSortField, direction: nextSortDirection }]
          : undefined,
        limit: Number(limit),
        offset: nextOffset,
      })

      setRowsState(result)
    } catch (error) {
      if (isAuthError(error)) {
        await handleAuthFailure()
        return
      }

      setRowsError(toExplorerError(error, 'Could not load rows.'))
    } finally {
      setRowsLoading(false)
    }
  }

  async function syncSchema(action: 'generate' | 'refresh') {
    if (!Number.isInteger(datasourceId) || datasourceId <= 0) {
      return
    }

    try {
      setSchemaSyncing(true)

      if (action === 'generate') {
        await generateSchema(datasourceId)
      } else {
        await refreshSchema(datasourceId)
      }

      await loadWorkspace(selectedTableName)
      setFeedback({
        tone: 'success',
        title: action === 'generate' ? 'Schema generated' : 'Schema refreshed',
        message: 'The latest tables and views are ready to browse.',
      })
    } catch (error) {
      if (isAuthError(error)) {
        await handleAuthFailure()
        return
      }

      setFeedback({
        tone: 'danger',
        title: action === 'generate' ? 'Schema generation failed' : 'Schema refresh failed',
        message: error instanceof Error ? error.message : 'Schema action failed.',
      })
    } finally {
      setSchemaSyncing(false)
    }
  }

  async function runAggregate() {
    if (!currentTable || !Number.isInteger(datasourceId) || datasourceId <= 0) {
      return
    }

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

      const filterPayload = buildFilterPayload(effectiveAggregateFilters, currentTable)
      const result = await aggregateRows(datasourceId, currentTable.qualifiedName, {
        selections,
        filter:
          Object.keys(filterPayload).length > 0 ? filterPayload : undefined,
      })

      setAggregateResult(result)
    } catch (error) {
      if (isAuthError(error)) {
        await handleAuthFailure()
        return
      }

      setAggregateError(
        error instanceof Error ? error.message : 'Aggregate query failed.'
      )
    } finally {
      setAggregateLoading(false)
    }
  }

  async function submitCreate() {
    if (!currentTable || !Number.isInteger(datasourceId) || datasourceId <= 0) {
      return
    }

    try {
      setMutating(true)
      const values = buildMutationValues(currentTable, createDraft, 'create')
      const response = await createRow(datasourceId, currentTable.qualifiedName, {
        values,
      })

      setFeedback({
        tone: 'success',
        title: 'Row created',
        message: `Inserted ${response.affectedRows} row${response.affectedRows === 1 ? '' : 's'} successfully.`,
      })
      setCreateDraft(buildEmptyDraft(currentTable))
      await runReadQuery()
    } catch (error) {
      if (isAuthError(error)) {
        await handleAuthFailure()
        return
      }

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
    if (
      !currentTable ||
      !selectedRow ||
      !primaryKeyColumn ||
      !Number.isInteger(datasourceId) ||
      datasourceId <= 0
    ) {
      return
    }

    try {
      setMutating(true)
      const primaryKeyValue = Number(selectedRow[primaryKeyColumn.name])

      if (Number.isNaN(primaryKeyValue)) {
        throw new Error('The selected row does not expose a numeric primary key.')
      }

      const values = buildMutationValues(
        currentTable,
        updateDraft,
        'update',
        selectedRow
      )

      await updateRow(datasourceId, currentTable.qualifiedName, primaryKeyValue, {
        values,
      })

      setFeedback({
        tone: 'success',
        title: 'Row updated',
        message: `Updated the record identified by ${primaryKeyColumn.name}=${primaryKeyValue}.`,
      })
      await runReadQuery()
    } catch (error) {
      if (isAuthError(error)) {
        await handleAuthFailure()
        return
      }

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
    if (
      !currentTable ||
      !pendingDelete ||
      !primaryKeyColumn ||
      !Number.isInteger(datasourceId) ||
      datasourceId <= 0
    ) {
      return
    }

    try {
      setMutating(true)
      const primaryKeyValue = Number(pendingDelete[primaryKeyColumn.name])

      if (Number.isNaN(primaryKeyValue)) {
        throw new Error('The selected row does not expose a numeric primary key.')
      }

      await deleteRow(datasourceId, currentTable.qualifiedName, primaryKeyValue)

      setFeedback({
        tone: 'success',
        title: 'Row deleted',
        message: `Deleted the record identified by ${primaryKeyColumn.name}=${primaryKeyValue}.`,
      })
      setPendingDelete(null)
      setSelectedRowIndex(null)
      await runReadQuery()
    } catch (error) {
      if (isAuthError(error)) {
        await handleAuthFailure()
        return
      }

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
    setWorkspaceMenuOpen(false)
    clearSavedDatasource()
    await logout()
    navigate('/login', { replace: true })
  }

  async function handleDeleteAccount() {
    try {
      setAccountDeleting(true)
      await deleteCurrentAccount()
      setPendingAccountDelete(false)
      setWorkspaceMenuOpen(false)
      clearSavedDatasource()
      await logout({ skipRequest: true })
      navigate('/login', {
        replace: true,
        state: {
          message: 'Your account was deleted successfully.',
        },
      })
    } catch (error) {
      if (isAuthError(error)) {
        await handleAuthFailure()
        return
      }

      setFeedback({
        tone: 'danger',
        title: 'Account deletion failed',
        message:
          error instanceof Error
            ? error.message
            : 'Could not delete the account right now.',
      })
    } finally {
      setAccountDeleting(false)
    }
  }

  return (
    <main ref={rootRef} className="page-shell py-6 sm:py-8">
      <div className="workspace-toolbar">
        <div className="flex w-full flex-wrap items-start justify-between gap-3 pointer-events-auto">
          <div className="flex flex-wrap items-center gap-3">
            <button
              type="button"
              className="secondary-button"
              onClick={() => navigate('/datasource')}
            >
              Datasources
            </button>
            <button
              type="button"
              className="secondary-button"
              onClick={() => syncSchema(schemaSummary ? 'refresh' : 'generate')}
              disabled={schemaSyncing}
            >
              {schemaSyncing
                ? 'Syncing...'
                : schemaSummary
                  ? 'Refresh schema'
                  : 'Generate schema'}
            </button>
          </div>

          <div ref={workspaceMenuRef} className="workspace-menu">
            <button
              type="button"
              className="workspace-menu-trigger"
              onClick={() => setWorkspaceMenuOpen((current) => !current)}
              aria-expanded={workspaceMenuOpen}
              aria-haspopup="menu"
            >
              <span>
                <span className="workspace-menu-label">Session</span>
                <span className="workspace-menu-value">
                  {user?.username ?? user?.email ?? 'Account options'}
                </span>
              </span>
              <span
                className={`workspace-menu-caret ${workspaceMenuOpen ? 'is-open' : ''}`}
              />
            </button>

            <div
              className={`workspace-menu-panel ${workspaceMenuOpen ? 'is-open' : ''}`}
              role="menu"
              aria-label="Session actions"
            >
              <div className="workspace-menu-summary">
                <p className="workspace-menu-summary-title">Signed in workspace</p>
                <p className="workspace-menu-summary-copy">
                  {user?.username ?? user?.email ?? 'Manage your current session and account.'}
                </p>
              </div>

              <button
                type="button"
                className="workspace-menu-action"
                role="menuitem"
                onClick={() => void handleLogOut()}
              >
                Log out
              </button>
              <button
                type="button"
                className="workspace-menu-action workspace-menu-action-danger"
                role="menuitem"
                onClick={() => {
                  setWorkspaceMenuOpen(false)
                  setPendingAccountDelete(true)
                }}
              >
                Delete account
              </button>
            </div>
          </div>
        </div>
      </div>

      <section className="surface-panel px-6 py-7 sm:px-8 sm:py-8 lg:px-10">
        <div className="relative z-10">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <span className="section-badge">Runtime explorer</span>
              <h1 className="display-title mt-6 text-[3rem] text-white sm:text-[4rem]">
                Explore live tables and rows.
              </h1>
              <p className="display-copy mt-4 max-w-3xl text-sm sm:text-base">
                Focus on the datasource, choose the tables that matter, then browse
                rows, run aggregates, and make row-level changes where the schema
                allows it.
              </p>
            </div>
            <div className="grid gap-3 xl:max-w-[17rem]">
              <div className="rounded-[24px] border border-white/8 bg-white/[0.03] px-4 py-4">
                <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">
                  Datasource
                </p>
                <p className="mt-3 text-lg font-semibold text-white">
                  {datasource?.displayName ?? 'Loading...'}
                </p>
                <p className="text-sm text-zinc-400">
                  {datasource
                    ? `${datasource.databaseName} / ${datasource.schemaName}`
                    : 'Waiting for metadata'}
                </p>
              </div>
            </div>
          </div>

          {feedback && (
            <div className={`mt-6 rounded-[24px] border p-4 ${feedbackClass(feedback.tone)}`}>
              <p className="text-sm font-semibold">{feedback.title}</p>
              <p className="mt-2 text-sm opacity-90">{feedback.message}</p>
            </div>
          )}
        </div>
      </section>

      {workspaceLoading ? (
        <section className="surface-panel mt-6 px-6 py-7 text-sm text-zinc-300">
          Loading datasource metadata and schema snapshot...
        </section>
      ) : !schemaSummary ? (
        <section className="surface-panel mt-6 px-6 py-7">
          <h2 className="text-2xl font-semibold text-white">No schema snapshot yet</h2>
          <p className="mt-3 max-w-2xl text-sm leading-7 text-zinc-300">
            Generate the schema snapshot first so the console can adapt to the live
            PostgreSQL structure at runtime.
          </p>
          <div className="mt-6 flex flex-wrap gap-3">
            <button
              type="button"
              className="primary-button"
              onClick={() => syncSchema('generate')}
              disabled={schemaSyncing}
            >
              {schemaSyncing ? 'Generating...' : 'Generate schema now'}
            </button>
            <button
              type="button"
              className="secondary-button"
              onClick={() => navigate('/datasource')}
            >
              Back to datasource management
            </button>
          </div>
        </section>
      ) : tables.length === 0 ? (
        <section className="surface-panel mt-6 px-6 py-7">
          <h2 className="text-2xl font-semibold text-white">No tables or views exposed</h2>
          <p className="mt-3 max-w-2xl text-sm leading-7 text-zinc-300">
            The schema snapshot exists, but it does not currently expose any
            supported tables or views for this datasource.
          </p>
        </section>
      ) : (
        <div className="mt-6 grid gap-6 xl:grid-cols-[minmax(18rem,24%)_minmax(0,46%)_minmax(20rem,30%)]">
          <aside className="space-y-6">
            <section className="surface-panel surface-overflow-visible px-5 py-6 sm:px-6">
              <div className="flex items-end justify-between gap-3">
                <div>
                  <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">
                    Schema explorer
                  </p>
                  <h2 className="mt-3 text-2xl font-semibold text-white">
                    Tables and views
                  </h2>
                </div>
                <span className="small-chip">{formatCount(filteredTables.length, 'shown result')}</span>
              </div>
              <p className="mt-3 text-sm text-zinc-400">
                {schemaFilter === SCHEMA_FILTER_ALL
                  ? 'Browsing all schemas returned by the backend.'
                  : schemaFilter === SCHEMA_FILTER_SYSTEM
                    ? 'Browsing internal and Supabase-managed schemas.'
                    : `Focused on ${schemaFilter || preferredSchema || 'the active schema'} by default.`}
                {hiddenSystemTables > 0 ? ` ${formatCount(hiddenSystemTables, 'internal resource')} hidden.` : ''}
              </p>
              <div className="mt-5 space-y-3">
                <AnimatedSelect
                  value={schemaFilter}
                  onChange={(value) => setSchemaFilter(value)}
                  options={schemaFilterOptions}
                  placeholder="Choose a schema"
                  ariaLabel="Schema filter"
                />
                <input
                  value={tableSearch}
                  onChange={(event) => setTableSearch(event.target.value)}
                  placeholder="Search table, view, or schema"
                  className="input-shell"
                />
              </div>
              <div className="mt-4 max-h-[62vh] space-y-4 overflow-auto pr-1">
                {groupedFilteredTables.length === 0 && (
                  <div className="rounded-[22px] border border-white/8 bg-white/[0.03] p-4 text-sm text-zinc-400">
                    No tables or views match the current schema focus and search.
                  </div>
                )}

                {groupedFilteredTables.map((group) => (
                  <div key={group.schemaName} className="space-y-3">
                    {(schemaFilter === SCHEMA_FILTER_ALL || schemaFilter === SCHEMA_FILTER_SYSTEM) && (
                      <div className="flex items-center justify-between gap-3 px-1">
                        <p className="text-xs uppercase tracking-[0.22em] text-zinc-500">
                          {group.schemaName}
                        </p>
                        <span className="small-chip">
                          {formatCount(group.tables.length, 'resource')}
                        </span>
                      </div>
                    )}

                    {group.tables.map((table) => (
                      <button
                        key={table.qualifiedName}
                        type="button"
                        className={`surface-card w-full p-4 text-left ${selectedTableName === table.qualifiedName ? 'ring-1 ring-cyan-300/30' : ''}`}
                        onClick={() =>
                          startTransition(() => setSelectedTableName(table.qualifiedName))
                        }
                      >
                        <div className="flex items-start justify-between gap-3">
                          <div className="min-w-0">
                            <p className="truncate text-sm font-semibold text-white">
                              {table.name}
                            </p>
                            <p className="mt-1 text-xs uppercase tracking-[0.22em] text-zinc-500">
                              {schemaFilter === SCHEMA_FILTER_ALL || schemaFilter === SCHEMA_FILTER_SYSTEM
                                ? `${group.schemaName} · ${getTableTypeLabel(table.tableType)}`
                                : getTableTypeLabel(table.tableType)}
                            </p>
                          </div>
                          <span className="small-chip">{formatCount(table.columns.length, 'column')}</span>
                        </div>
                        <div className="mt-3 flex flex-wrap gap-2">
                          {getActiveCapabilities(table).map((capability) => (
                            <span key={capability.key} className="mini-capability is-active">
                              {capability.label}
                            </span>
                          ))}
                        </div>
                      </button>
                    ))}
                  </div>
                ))}
              </div>
            </section>

            {currentTable && (
              <section className="surface-panel px-5 py-6 sm:px-6">
                <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">
                  Selected table
                </p>
                <h2 className="mt-3 text-2xl font-semibold text-white">
                  {currentTable.name}
                </h2>
                <p className="mt-2 text-sm text-zinc-400">{currentTable.qualifiedName}</p>
                <div className="mt-4 flex flex-wrap gap-2">
                  {activeCapabilities.map((capability) => (
                    <span key={capability.key} className="mini-capability is-active">
                      {capability.label}
                    </span>
                  ))}
                </div>
                <div className="mt-5 grid gap-3">
                  <DetailRow
                    label="Primary key"
                    value={currentTable.primaryKey?.columns.join(', ') ?? 'None'}
                  />
                  <DetailRow
                    label="Columns"
                    value={formatCount(currentTable.columns.length, 'column')}
                  />
                  <DetailRow
                    label="Relations"
                    value={formatCount(currentTable.relations.length, 'relation')}
                  />
                </div>

                <div className="mt-5">
                  <p className="text-xs uppercase tracking-[0.2em] text-zinc-500">
                    Columns
                  </p>
                  <div className="mt-3 max-h-[16rem] space-y-2 overflow-auto pr-1">
                    {getReadableColumns(currentTable).map((column) => (
                      <div
                        key={column.name}
                        className="rounded-[18px] border border-white/8 bg-white/[0.03] p-3"
                      >
                        <div className="flex items-start justify-between gap-3">
                          <div className="min-w-0">
                            <p className="truncate text-sm font-semibold text-white">
                              {column.name}
                            </p>
                            <p className="mt-1 text-xs text-zinc-400">{column.dbType}</p>
                          </div>
                          <div className="flex flex-wrap justify-end gap-2">
                            {currentTable.primaryKey?.columns.includes(column.name) && (
                              <span className="small-chip">PK</span>
                            )}
                            {column.capabilities.writable && !column.identity && !column.generated && (
                              <span className="small-chip">Editable</span>
                            )}
                            {!column.capabilities.writable && (
                              <span className="small-chip">Read only</span>
                            )}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="mt-5">
                  <p className="text-xs uppercase tracking-[0.2em] text-zinc-500">
                    Relations
                  </p>
                  <div className="mt-3 space-y-2">
                    {currentTable.relations.length === 0 ? (
                      <div className="rounded-[18px] border border-white/8 bg-white/[0.03] p-4 text-sm text-zinc-400">
                        No relations were detected for this table.
                      </div>
                    ) : (
                      currentTable.relations.map((relation) => (
                        <div
                          key={relation.name}
                          className="rounded-[18px] border border-white/8 bg-white/[0.03] p-3"
                        >
                          <p className="text-sm font-semibold text-white">
                            {relation.targetQualifiedName}
                          </p>
                          <p className="mt-1 text-xs uppercase tracking-[0.22em] text-zinc-500">
                            {relation.relationType.replaceAll('_', ' ')}
                          </p>
                          <p className="mt-2 text-sm text-zinc-400">
                            {relation.sourceColumns.join(', ')} {'->'} {relation.targetColumns.join(', ')}
                          </p>
                        </div>
                      ))
                    )}
                  </div>
                </div>
              </section>
            )}
          </aside>

          <section className="space-y-6">
            {currentTable && (
              <>
                <section className="surface-panel surface-overflow-visible px-5 py-6 sm:px-6">
                  <div className="flex flex-wrap items-end justify-between gap-4">
                    <div>
                      <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">
                        Rows explorer
                      </p>
                      <h2 className="mt-3 text-2xl font-semibold text-white">
                        {currentTable.name}
                      </h2>
                      <p className="mt-2 text-sm text-zinc-400">
                        {currentTable.qualifiedName}
                      </p>
                    </div>
                    <div className="flex flex-wrap gap-3">
                      <span className="small-chip">
                        {getTableTypeLabel(currentTable.tableType)}
                      </span>
                      <button
                        type="button"
                        className="secondary-button"
                        onClick={() => {
                          setOffset(0)
                          void runReadQuery(currentTable, 0)
                        }}
                        disabled={rowsLoading}
                      >
                        {rowsLoading ? 'Loading...' : 'Refresh rows'}
                      </button>
                    </div>
                  </div>

                  <div className="mt-5 grid gap-4 lg:grid-cols-[minmax(0,1fr)_180px_180px]">
                    <div className="surface-card p-4">
                      <p className="text-xs uppercase tracking-[0.2em] text-zinc-500">
                        Columns shown
                      </p>
                      <div className="mt-4 flex flex-wrap gap-2">
                        {getReadableColumns(currentTable).map((column) => {
                          const active = visibleColumns.includes(column.name)

                          return (
                            <button
                              key={column.name}
                              type="button"
                              className={`chip-toggle ${active ? 'is-active' : ''}`}
                              onClick={() =>
                                setVisibleColumns((current) => {
                                  if (active) {
                                    if (current.length === 1) {
                                      return current
                                    }

                                    return current.filter((item) => item !== column.name)
                                  }

                                  return [...current, column.name]
                                })
                              }
                            >
                              {column.name}
                            </button>
                          )
                        })}
                      </div>
                    </div>

                    <div className="surface-card p-4">
                      <p className="text-xs uppercase tracking-[0.2em] text-zinc-500">
                        Sort
                      </p>
                      <div className="mt-3">
                        <AnimatedSelect
                          value={sortField}
                          onChange={(value) => setSortField(value)}
                          options={getReadableColumns(currentTable)
                            .filter((column) => column.capabilities.sortable)
                            .map((column) => ({
                              value: column.name,
                              label: column.name,
                            }))}
                          placeholder="No sort"
                          ariaLabel="Sort field"
                        />
                      </div>
                      <div className="mt-3">
                        <AnimatedSelect
                          value={sortDirection}
                          onChange={(value) => setSortDirection(value as SortDirection)}
                          options={SORT_DIRECTIONS}
                          ariaLabel="Sort direction"
                          disabled={!sortField}
                        />
                      </div>
                    </div>

                    <div className="surface-card p-4">
                      <p className="text-xs uppercase tracking-[0.2em] text-zinc-500">
                        Pagination
                      </p>
                      <div className="mt-3">
                        <AnimatedSelect
                          value={limit}
                          onChange={(value) => setLimit(value)}
                          options={PAGE_SIZE_OPTIONS.map((value) => ({
                            value,
                            label: `${value} rows`,
                          }))}
                          ariaLabel="Page size"
                        />
                      </div>
                      <div className="mt-3 flex gap-2">
                        <button
                          type="button"
                          className="secondary-button px-4 py-2"
                          onClick={() => {
                            const next = Math.max(0, offset - Number(limit))
                            setOffset(next)
                            void runReadQuery(currentTable, next)
                          }}
                          disabled={rowsLoading || offset === 0}
                        >
                          Prev
                        </button>
                        <button
                          type="button"
                          className="secondary-button px-4 py-2"
                          onClick={() => {
                            const next = offset + Number(limit)
                            setOffset(next)
                            void runReadQuery(currentTable, next)
                          }}
                          disabled={
                            rowsLoading ||
                            (rowsState?.page.returnedCount ?? 0) < Number(limit)
                          }
                        >
                          Next
                        </button>
                      </div>
                    </div>
                  </div>

                  <div className="mt-5 surface-card p-4">
                    <div className="flex items-center justify-between gap-3">
                      <p className="text-xs uppercase tracking-[0.2em] text-zinc-500">
                        Filters
                      </p>
                      <button
                        type="button"
                        className="secondary-button px-4 py-2"
                        onClick={() =>
                          setFilters((current) => [
                            ...current,
                            {
                              id: nextId(),
                              field: filterableColumns[0]?.name ?? '',
                              operator: filterableColumns[0]
                                ? getAllowedOperators(filterableColumns[0])[0]
                                : 'eq',
                              value: '',
                              secondaryValue: '',
                            },
                          ])
                        }
                        disabled={filterableColumns.length === 0}
                      >
                        Add filter
                      </button>
                    </div>
                    <div className="mt-4 space-y-3">
                      {filters.length === 0 && (
                        <div className="rounded-[18px] border border-white/8 bg-white/[0.03] p-4 text-sm text-zinc-400">
                          No active filters yet.
                        </div>
                      )}
                      {filters.map((filter) => (
                        <FilterRow
                          key={filter.id}
                          table={currentTable}
                          value={filter}
                          onChange={(next) =>
                            setFilters((current) =>
                              current.map((item) => (item.id === filter.id ? next : item))
                            )
                          }
                          onRemove={() =>
                            setFilters((current) =>
                              current.filter((item) => item.id !== filter.id)
                            )
                          }
                        />
                      ))}
                    </div>
                  </div>

                  <div className="mt-5 flex flex-wrap gap-3">
                    <span className="small-chip">
                      {formatCount(rowsState?.page.returnedCount ?? 0, 'row')}
                    </span>
                    <span className="small-chip">
                      Limit {rowsState?.page.limit ?? Number(limit)}
                    </span>
                    <span className="small-chip">
                      Offset {rowsState?.page.offset ?? offset}
                    </span>
                  </div>

                  <div className="mt-5 overflow-hidden rounded-[24px] border border-white/8">
                    {rowsError && (
                      <div className="border-b border-red-500/20 bg-red-500/8 px-4 py-3 text-sm text-red-100">
                        <p className="font-medium text-red-50">{rowsError.title}</p>
                        <p className="mt-1">{rowsError.message}</p>
                        {rowsError.detail && (
                          <p className="mt-2 text-xs text-red-200/80">{rowsError.detail}</p>
                        )}
                      </div>
                    )}
                    <div className="overflow-auto">
                      <table className="data-table w-full min-w-[760px]">
                        <thead>
                          <tr>
                            {visibleTableColumns.map((column) => (
                              <th key={column.name}>{column.name}</th>
                            ))}
                          </tr>
                        </thead>
                        <tbody>
                          {!rowsLoading && currentRows.length === 0 && (
                            <tr>
                              <td
                                colSpan={Math.max(visibleTableColumns.length, 1)}
                                className="text-center text-zinc-400"
                              >
                                No rows match the current request.
                              </td>
                            </tr>
                          )}
                          {currentRows.map((row, index) => (
                            <tr
                              key={`${index}-${safeString(row[primaryKeyColumn?.name ?? index])}`}
                              className={selectedRowIndex === index ? 'is-selected' : ''}
                              onClick={() => setSelectedRowIndex(index)}
                            >
                              {visibleTableColumns.map((column) => (
                                <td key={column.name}>{formatCellValue(row[column.name])}</td>
                              ))}
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                </section>
              </>
            )}
          </section>
          <aside className="space-y-6">
            {currentTable && (
              <>
                <section className="surface-panel surface-overflow-visible px-5 py-6 sm:px-6">
                  <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">
                    Aggregates
                  </p>
                  <h2 className="mt-3 text-2xl font-semibold text-white">
                    Counts and rollups
                  </h2>
                  <p className="mt-3 text-sm text-zinc-400">
                    Summarize the selected table using the same schema-aware runtime.
                  </p>
                  <div className="mt-5 space-y-3">
                    <div className="surface-card p-4">
                      <div className="flex items-center justify-between gap-3">
                        <p className="text-xs uppercase tracking-[0.2em] text-zinc-500">
                          Aggregate filters
                        </p>
                        <button
                          type="button"
                          className="secondary-button px-4 py-2"
                          onClick={() =>
                            setAggregateFilters((current) => [
                              ...current,
                              {
                                id: nextId(),
                                field: filterableColumns[0]?.name ?? '',
                                operator: filterableColumns[0]
                                  ? getAllowedOperators(filterableColumns[0])[0]
                                  : 'eq',
                                value: '',
                                secondaryValue: '',
                              },
                            ])
                          }
                          disabled={filterableColumns.length === 0}
                        >
                          Add filter
                        </button>
                      </div>
                      <p className="mt-3 text-sm text-zinc-400">
                        Leave this empty to reuse the filters from the rows explorer.
                      </p>
                      <div className="mt-4 space-y-3">
                        {aggregateFilters.map((filter) => (
                          <FilterRow
                            key={filter.id}
                            table={currentTable}
                            value={filter}
                            onChange={(next) =>
                              setAggregateFilters((current) =>
                                current.map((item) =>
                                  item.id === filter.id ? next : item
                                )
                              )
                            }
                            onRemove={() =>
                              setAggregateFilters((current) =>
                                current.filter((item) => item.id !== filter.id)
                              )
                            }
                          />
                        ))}
                      </div>
                    </div>
                    {aggregateSelections.map((selectionItem) => (
                      <div
                        key={selectionItem.id}
                        className="surface-card p-4"
                      >
                        <div className="grid gap-3">
                          <AnimatedSelect
                            value={selectionItem.fn}
                            onChange={(value) =>
                              setAggregateSelections((current) =>
                                current.map((item) =>
                                  item.id === selectionItem.id
                                    ? {
                                        ...item,
                                        fn: value as AggregateFunction,
                                      }
                                    : item
                                )
                              )
                            }
                            options={AGGREGATE_OPTIONS}
                            ariaLabel="Aggregate function"
                          />
                          <AnimatedSelect
                            value={selectionItem.field}
                            onChange={(value) =>
                              setAggregateSelections((current) =>
                                current.map((item) =>
                                  item.id === selectionItem.id
                                    ? { ...item, field: value }
                                    : item
                                )
                              )
                            }
                            options={getReadableColumns(currentTable)
                              .filter((column) =>
                                getAllowedAggregates(column).includes(selectionItem.fn)
                              )
                              .map((column) => ({
                                value: column.name,
                                label: column.name,
                              }))}
                            placeholder={
                              selectionItem.fn === 'count'
                                ? 'Optional target column'
                                : 'Select target column'
                            }
                            ariaLabel="Aggregate target"
                          />
                          <input
                            value={selectionItem.alias}
                            onChange={(event) =>
                              setAggregateSelections((current) =>
                                current.map((item) =>
                                  item.id === selectionItem.id
                                    ? { ...item, alias: event.target.value }
                                    : item
                                )
                              )
                            }
                            placeholder="Alias"
                            className="input-shell"
                          />
                        </div>
                      </div>
                    ))}
                    <div className="flex flex-wrap gap-3">
                      <button
                        type="button"
                        className="secondary-button"
                        onClick={() =>
                          setAggregateSelections((current) => [
                            ...current,
                            { id: nextId(), fn: 'count', field: '', alias: '' },
                          ])
                        }
                      >
                        Add aggregate
                      </button>
                      <button
                        type="button"
                        className="primary-button"
                        onClick={runAggregate}
                        disabled={aggregateLoading}
                      >
                        {aggregateLoading ? 'Running...' : 'Run aggregate'}
                      </button>
                    </div>
                    {aggregateError && (
                      <div className="rounded-[18px] border border-red-500/20 bg-red-500/8 p-4 text-sm text-red-100">
                        {aggregateError}
                      </div>
                    )}
                    {aggregateResult?.rows?.[0] && (
                      <div className="grid gap-3">
                        {Object.entries(aggregateResult.rows[0].values).map(
                          ([key, value]) => (
                            <div key={key} className="surface-card p-4">
                              <p className="text-xs uppercase tracking-[0.2em] text-zinc-500">
                                {key}
                              </p>
                              <p className="mt-2 text-2xl font-semibold text-white">
                                {formatCellValue(value)}
                              </p>
                            </div>
                          )
                        )}
                      </div>
                    )}
                  </div>
                </section>

                {mutationSupported && (
                  <section className="surface-panel surface-overflow-visible px-5 py-6 sm:px-6">
                    <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">
                      Row changes
                    </p>
                    <h2 className="mt-3 text-2xl font-semibold text-white">
                      Create, update, delete
                    </h2>
                    <p className="mt-3 text-sm text-zinc-400">
                      Only the actions supported by this table are shown here.
                    </p>
                    <div className="mt-5 flex flex-wrap gap-3">
                      {mutationModeOptions.map((option) => (
                        <button
                          key={option.value}
                          type="button"
                          className={`mode-toggle ${mutationMode === option.value ? 'is-active' : ''}`}
                          onClick={() => setMutationMode(option.value)}
                          disabled={!option.enabled}
                        >
                          <span className="mode-toggle-label">{option.label}</span>
                          <span className="mode-toggle-copy">{option.description}</span>
                        </button>
                      ))}
                    </div>
                    <div className="mt-4">
                      {mutationMode === 'create' && canCreate && editableMutationColumns.length > 0 && (
                        <div className="surface-card p-4">
                          <MutationFields
                            title="Create row"
                            table={currentTable}
                            draft={createDraft}
                            onChange={setCreateDraft}
                          />
                          <button
                            type="button"
                            className="primary-button mt-4 w-full"
                            onClick={submitCreate}
                            disabled={mutating}
                          >
                            {mutating ? 'Saving...' : 'Create row'}
                          </button>
                        </div>
                      )}

                      {mutationMode === 'update' && canEditRows && (
                        <div className="surface-card p-4">
                          {!selectedRow ? (
                            <div className="rounded-[18px] border border-white/8 bg-white/[0.03] p-4 text-sm text-zinc-400">
                              Select a row in the grid to update it.
                            </div>
                          ) : (
                            <>
                              <MutationFields
                                title="Update selected row"
                                table={currentTable}
                                draft={updateDraft}
                                onChange={setUpdateDraft}
                              />
                              <button
                                type="button"
                                className="secondary-button mt-4 w-full"
                                onClick={submitUpdate}
                                disabled={mutating}
                              >
                                Update selected row
                              </button>
                            </>
                          )}
                        </div>
                      )}

                      {mutationMode === 'delete' && canDeleteRows && (
                        <div className="surface-card p-4">
                          {!selectedRow ? (
                            <div className="rounded-[18px] border border-white/8 bg-white/[0.03] p-4 text-sm text-zinc-400">
                              Select a row in the grid to delete it.
                            </div>
                          ) : (
                            <>
                              <div className="rounded-[18px] border border-red-400/14 bg-red-400/[0.05] p-4">
                                <p className="text-sm font-semibold text-red-50">
                                  Delete selected row
                                </p>
                                <p className="mt-2 text-sm leading-6 text-red-100/82">
                                  This removes the selected record from the live table.
                                  You’ll get one final confirmation before anything is deleted.
                                </p>
                              </div>
                              <button
                                type="button"
                                className="secondary-button mt-4 w-full text-red-100"
                                onClick={() => setPendingDelete(selectedRow)}
                                disabled={mutating}
                              >
                                Delete selected row
                              </button>
                            </>
                          )}
                        </div>
                      )}
                    </div>
                  </section>
                )}
              </>
            )}
          </aside>
        </div>
      )}

      {pendingDelete && primaryKeyColumn && (
        <div className="fixed inset-0 z-[120] flex items-center justify-center bg-slate-950/70 px-4 backdrop-blur-md">
          <div className="surface-panel w-full max-w-md px-6 py-6">
            <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">
              Confirm delete
            </p>
            <h2 className="mt-3 text-2xl font-semibold text-white">
              Delete row {safeString(pendingDelete[primaryKeyColumn.name])}?
            </h2>
            <p className="mt-4 text-sm leading-6 text-zinc-300">
              This action removes a single row identified by the primary key. Bulk
              deletes are intentionally not available.
            </p>
            <div className="mt-6 flex flex-wrap justify-end gap-3">
              <button
                type="button"
                className="secondary-button"
                onClick={() => setPendingDelete(null)}
              >
                Cancel
              </button>
              <button
                type="button"
                className="primary-button"
                onClick={submitDelete}
                disabled={mutating}
              >
                {mutating ? 'Deleting...' : 'Delete row'}
              </button>
            </div>
          </div>
        </div>
      )}

      {pendingAccountDelete && (
        <div className="fixed inset-0 z-[125] flex items-center justify-center bg-slate-950/75 px-4 backdrop-blur-md">
          <div className="surface-panel w-full max-w-lg px-6 py-7 sm:px-8">
            <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">
              Account removal
            </p>
            <h2 className="mt-3 text-3xl font-semibold text-white">
              Are you sure you want to delete your account?
            </h2>
            <p className="mt-4 text-sm leading-7 text-zinc-300">
              This will permanently remove your profile, datasources, and active
              sessions from ForgeQL. This action cannot be undone.
            </p>
            <div className="mt-5 flex flex-wrap gap-2">
              <span className="small-chip">Datasources removed</span>
              <span className="small-chip">Sessions revoked</span>
              <span className="small-chip">Permanent action</span>
            </div>
            <div className="mt-7 flex flex-wrap justify-end gap-3">
              <button
                type="button"
                className="secondary-button"
                onClick={() => setPendingAccountDelete(false)}
                disabled={accountDeleting}
              >
                Keep account
              </button>
              <button
                type="button"
                className="primary-button"
                onClick={() => void handleDeleteAccount()}
                disabled={accountDeleting}
              >
                {accountDeleting ? 'Deleting account...' : 'Delete account'}
              </button>
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
        No filterable columns are exposed for this table.
      </div>
    )
  }

  const operators = getAllowedOperators(column)
  const operator = operators.includes(value.operator) ? value.operator : operators[0]

  return (
    <div className="grid gap-3 rounded-[20px] border border-white/8 bg-white/[0.03] p-4">
      <AnimatedSelect
        value={value.field}
        onChange={(field) =>
          onChange({
            ...value,
            field,
            operator: getAllowedOperators(getColumnByName(table, field) ?? column)[0] ?? 'eq',
          })
        }
        options={filterableColumns.map((item) => ({
          value: item.name,
          label: item.name,
          description: getColumnCategory(item),
        }))}
        ariaLabel="Filter field"
      />
      <AnimatedSelect
        value={operator}
        onChange={(next) => onChange({ ...value, operator: next as FilterOperator })}
        options={FILTER_OPERATOR_OPTIONS.filter((item) => operators.includes(item.value))}
        ariaLabel="Filter operator"
      />
      {isValueRequired(operator) && (
        <input
          value={value.value}
          onChange={(event) => onChange({ ...value, value: event.target.value })}
          placeholder={supportsListValue(operator) ? 'Comma-separated values' : 'Value'}
          className="input-shell"
        />
      )}
      {supportsSecondValue(operator) && (
        <input
          value={value.secondaryValue}
          onChange={(event) =>
            onChange({ ...value, secondaryValue: event.target.value })
          }
          placeholder="Second value"
          className="input-shell"
        />
      )}
      <button type="button" className="secondary-button px-4 py-2" onClick={onRemove}>
        Remove
      </button>
    </div>
  )
}

function MutationFields({
  title,
  table,
  draft,
  onChange,
}: {
  title: string
  table: SchemaTable
  draft: Record<string, string>
  onChange: (next: Record<string, string>) => void
}) {
  const editableColumns = getEditableMutationColumns(table)

  return (
    <div>
      <p className="text-sm font-semibold text-white">{title}</p>
      <div className="mt-3 space-y-3">
        {editableColumns.length === 0 && (
          <div className="rounded-[18px] border border-white/8 bg-white/[0.03] p-4 text-sm text-zinc-400">
            No user-editable columns are available for this table.
          </div>
        )}
        {editableColumns.map((column) => {
          const category = getColumnCategory(column)
          const required = getRequiredColumns(table).some(
            (item) => item.name === column.name
          )

          return (
            <div key={column.name} className="space-y-2">
              <label className="text-sm font-medium text-zinc-300">
                {column.name}
                {required ? ' *' : ''}
              </label>
              {category === 'enum' ? (
                <AnimatedSelect
                  value={draft[column.name] ?? ''}
                  onChange={(value) => onChange({ ...draft, [column.name]: value })}
                  options={column.enumLabels.map((value) => ({ value, label: value }))}
                  placeholder="Select a value"
                  ariaLabel={column.name}
                />
              ) : category === 'boolean' ? (
                <AnimatedSelect
                  value={draft[column.name] ?? ''}
                  onChange={(value) => onChange({ ...draft, [column.name]: value })}
                  options={[
                    { value: 'true', label: 'true' },
                    { value: 'false', label: 'false' },
                  ]}
                  placeholder="Choose true or false"
                  ariaLabel={column.name}
                />
              ) : category === 'json' || category === 'array' ? (
                <textarea
                  value={draft[column.name] ?? ''}
                  onChange={(event) =>
                    onChange({ ...draft, [column.name]: event.target.value })
                  }
                  className="input-shell min-h-[110px] resize-y"
                />
              ) : (
                <input
                  value={draft[column.name] ?? ''}
                  onChange={(event) =>
                    onChange({ ...draft, [column.name]: event.target.value })
                  }
                  type={
                    category === 'number'
                      ? 'number'
                      : category === 'date'
                        ? 'date'
                        : category === 'time'
                          ? 'time'
                          : category === 'datetime'
                            ? 'datetime-local'
                            : 'text'
                  }
                  className="input-shell"
                />
              )}
            </div>
          )
        })}
      </div>
    </div>
  )
}
