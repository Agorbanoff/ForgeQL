import type {
  AggregateFunction,
  DatasourceConnectionStatus,
  FilterOperator,
  SchemaColumn,
  SchemaTable,
  SchemaTableType,
} from '../types/platform'

export const FILTER_OPERATOR_OPTIONS: Array<{
  value: FilterOperator
  label: string
}> = [
  { value: 'eq', label: 'Equals' },
  { value: 'ne', label: 'Not equal' },
  { value: 'gt', label: 'Greater than' },
  { value: 'gte', label: 'Greater or equal' },
  { value: 'lt', label: 'Less than' },
  { value: 'lte', label: 'Less or equal' },
  { value: 'in', label: 'In list' },
  { value: 'between', label: 'Between' },
  { value: 'like', label: 'Like' },
  { value: 'ilike', label: 'Case-insensitive like' },
  { value: 'isNull', label: 'Is null' },
  { value: 'isNotNull', label: 'Is not null' },
]

export const AGGREGATE_OPTIONS: Array<{
  value: AggregateFunction
  label: string
}> = [
  { value: 'count', label: 'Count' },
  { value: 'sum', label: 'Sum' },
  { value: 'avg', label: 'Average' },
  { value: 'min', label: 'Minimum' },
  { value: 'max', label: 'Maximum' },
]

export function getStatusTone(status: DatasourceConnectionStatus | null) {
  switch (status) {
    case 'SUCCEEDED':
      return 'success'
    case 'FAILED':
      return 'danger'
    case 'TIMED_OUT':
      return 'warning'
    default:
      return 'neutral'
  }
}

export function getStatusLabel(status: DatasourceConnectionStatus | null) {
  switch (status) {
    case 'SUCCEEDED':
      return 'Connected'
    case 'FAILED':
      return 'Failed'
    case 'TIMED_OUT':
      return 'Timed out'
    default:
      return 'Untested'
  }
}

export function getTableTypeLabel(tableType: SchemaTableType) {
  switch (tableType) {
    case 'MATERIALIZED_VIEW':
      return 'Materialized view'
    case 'VIEW':
      return 'View'
    default:
      return 'Table'
  }
}

export function getColumnByName(table: SchemaTable, columnName: string) {
  return table.columns.find((column) => column.name === columnName) ?? null
}

export function getPrimaryKeyColumn(table: SchemaTable) {
  const primaryKeyName = table.primaryKey?.columns?.[0]
  if (!primaryKeyName || table.primaryKey?.columns.length !== 1) {
    return null
  }

  return getColumnByName(table, primaryKeyName)
}

export function hasSingleNumericPrimaryKey(table: SchemaTable) {
  const primaryKeyColumn = getPrimaryKeyColumn(table)
  return Boolean(primaryKeyColumn?.numericType)
}

export function getWritableColumns(table: SchemaTable) {
  return table.columns.filter((column) => column.capabilities.writable)
}

export function getRequiredColumns(table: SchemaTable) {
  return getWritableColumns(table).filter(
    (column) =>
      !column.nullable &&
      !column.identity &&
      !column.generated &&
      column.defaultValue == null
  )
}

export function getReadableColumns(table: SchemaTable) {
  return [...table.columns].sort((left, right) => left.position - right.position)
}

export function getColumnCategory(column: SchemaColumn) {
  if (column.enumType) {
    return 'enum'
  }

  if (column.jsonType || column.jsonbType) {
    return 'json'
  }

  if (column.timestampWithTimeZone || column.timestampWithoutTimeZone) {
    return 'datetime'
  }

  if (/date/i.test(column.dbType) && !/update|time/i.test(column.dbType)) {
    return 'date'
  }

  if (/time/i.test(column.dbType) && !/timestamp/i.test(column.dbType)) {
    return 'time'
  }

  if (column.numericType) {
    return 'number'
  }

  if (/bool/i.test(column.dbType) || /Boolean/i.test(column.javaType ?? '')) {
    return 'boolean'
  }

  if (column.uuidType) {
    return 'uuid'
  }

  if (column.arrayType) {
    return 'array'
  }

  return 'text'
}

export function getAllowedOperators(column: SchemaColumn): FilterOperator[] {
  const category = getColumnCategory(column)

  if (category === 'boolean') {
    return ['eq', 'ne', 'isNull', 'isNotNull']
  }

  if (category === 'enum') {
    return ['eq', 'ne', 'in', 'isNull', 'isNotNull']
  }

  if (category === 'json' || category === 'array') {
    return ['eq', 'ne', 'isNull', 'isNotNull']
  }

  if (category === 'number' || category === 'date' || category === 'datetime' || category === 'time') {
    return ['eq', 'ne', 'gt', 'gte', 'lt', 'lte', 'between', 'in', 'isNull', 'isNotNull']
  }

  return ['eq', 'ne', 'like', 'ilike', 'in', 'isNull', 'isNotNull']
}

export function getAllowedAggregates(column: SchemaColumn): AggregateFunction[] {
  const category = getColumnCategory(column)

  if (category === 'number') {
    return ['count', 'sum', 'avg', 'min', 'max']
  }

  if (category === 'date' || category === 'datetime' || category === 'time') {
    return ['count', 'min', 'max']
  }

  return ['count']
}

export function isValueRequired(operator: FilterOperator) {
  return operator !== 'isNull' && operator !== 'isNotNull'
}

export function supportsSecondValue(operator: FilterOperator) {
  return operator === 'between'
}

export function supportsListValue(operator: FilterOperator) {
  return operator === 'in'
}

export function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return 'Not available'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }

  return date.toLocaleString()
}

export function formatShortDate(value: string | null | undefined) {
  if (!value) {
    return 'Unavailable'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }

  return date.toLocaleDateString(undefined, {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  })
}

export function formatCellValue(value: unknown) {
  if (value === null || value === undefined) {
    return 'null'
  }

  if (typeof value === 'boolean') {
    return value ? 'true' : 'false'
  }

  if (typeof value === 'number') {
    return Number.isFinite(value) ? value.toLocaleString() : String(value)
  }

  if (typeof value === 'string') {
    return value
  }

  return JSON.stringify(value)
}

export function parseDraftValue(column: SchemaColumn, raw: string): unknown {
  const trimmed = raw.trim()
  const category = getColumnCategory(column)

  if (trimmed === '') {
    return null
  }

  if (category === 'number') {
    return Number(trimmed)
  }

  if (category === 'boolean') {
    return trimmed === 'true'
  }

  if (category === 'json' || category === 'array') {
    return JSON.parse(trimmed)
  }

  return trimmed
}

export function serializeValueForInput(column: SchemaColumn, value: unknown) {
  const category = getColumnCategory(column)

  if (value === null || value === undefined) {
    return ''
  }

  if (category === 'json' || category === 'array') {
    return JSON.stringify(value, null, 2)
  }

  if (
    category === 'datetime' &&
    typeof value === 'string' &&
    value.includes('T')
  ) {
    return value.slice(0, 16)
  }

  return String(value)
}

export function formatCapabilityLabel(table: SchemaTable) {
  const writable = table.capabilities.insert || table.capabilities.update || table.capabilities.delete
  return writable ? 'Writable surface' : 'Read-only surface'
}
