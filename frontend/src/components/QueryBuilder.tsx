import { useMemo, useState } from 'react'
import { schema } from '../data/schema'

type EntityKey = keyof typeof schema.entities
type FilterOperator = 'eq' | 'ne' | 'gt' | 'gte' | 'lt' | 'lte' | 'like'

type FilterRow = {
  id: number
  field: string
  operator: FilterOperator
  value: string
}

type QueryBuilderProps = {
  onRunQuery: (payload: unknown) => void | Promise<void>
  loading: boolean
}

export function QueryBuilder({
  onRunQuery,
  loading,
}: QueryBuilderProps) {
  const entityNames = Object.keys(schema.entities) as EntityKey[]

  const [selectedEntity, setSelectedEntity] = useState<EntityKey>('users')
  const [selectedFields, setSelectedFields] = useState<string[]>([])
  const [filters, setFilters] = useState<FilterRow[]>([])
  const [limit, setLimit] = useState('')

  const entity = schema.entities[selectedEntity]

  function toggleField(field: string) {
    setSelectedFields((prev) =>
      prev.includes(field)
        ? prev.filter((f) => f !== field)
        : [...prev, field]
    )
  }

  function addFilter() {
    setFilters((prev) => [
      ...prev,
      {
        id: Date.now() + prev.length,
        field: entity.fields[0] ?? '',
        operator: 'eq',
        value: '',
      },
    ])
  }

  function updateFilter(id: number, patch: Partial<FilterRow>) {
    setFilters((prev) =>
      prev.map((filter) =>
        filter.id === id ? { ...filter, ...patch } : filter
      )
    )
  }

  function removeFilter(id: number) {
    setFilters((prev) => prev.filter((filter) => filter.id !== id))
  }

  const query = useMemo(
    () => {
      const filterPayload = filters.reduce<Record<string, Record<string, unknown>>>(
        (acc, filter) => {
          if (!filter.field || !filter.value.trim()) {
            return acc
          }

          acc[filter.field] = {
            ...(acc[filter.field] ?? {}),
            [filter.operator]: parseFilterValue(filter.value),
          }

          return acc
        },
        {}
      )

      return {
        entity: selectedEntity,
        fields: selectedFields,
        ...(Object.keys(filterPayload).length > 0 ? { filter: filterPayload } : {}),
        ...(limit.trim() && !Number.isNaN(Number(limit))
          ? { limit: Number(limit) }
          : {}),
      }
    },
    [filters, limit, selectedEntity, selectedFields]
  )

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between gap-3">
        <h2 className="text-lg font-semibold">Query Builder</h2>

        <button
          type="button"
          onClick={() => onRunQuery(query)}
          disabled={loading || selectedFields.length === 0}
          className="rounded-lg bg-cyan-500 px-4 py-2 text-sm font-medium text-black disabled:cursor-not-allowed disabled:opacity-50"
        >
          {loading ? 'Running...' : 'Run Query'}
        </button>
      </div>

      <div>
        <label className="text-sm text-zinc-400">Entity</label>
        <select
          value={selectedEntity}
          onChange={(e) => {
            setSelectedEntity(e.target.value as EntityKey)
            setSelectedFields([])
            setFilters([])
          }}
          className="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-800 p-2 text-white outline-none"
        >
          {entityNames.map((entityName) => (
            <option key={entityName} value={entityName}>
              {entityName}
            </option>
          ))}
        </select>
      </div>

      <div>
        <p className="mb-2 text-sm text-zinc-400">Fields</p>
        <div className="flex flex-wrap gap-2">
          {entity.fields.map((field) => (
            <button
              key={field}
              type="button"
              onClick={() => toggleField(field)}
              className={`rounded-md border px-2 py-1 text-sm ${
                selectedFields.includes(field)
                  ? 'border-cyan-400 bg-cyan-500 text-black'
                  : 'border-zinc-700 text-zinc-300'
              }`}
            >
              {field}
            </button>
          ))}
        </div>
      </div>

      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <p className="text-sm text-zinc-400">Filters</p>
          <button
            type="button"
            onClick={addFilter}
            className="rounded-lg border border-zinc-700 px-3 py-1 text-xs text-zinc-200"
          >
            Add filter
          </button>
        </div>

        {filters.length === 0 && (
          <div className="rounded-xl border border-zinc-800 bg-zinc-950 p-3 text-sm text-zinc-500">
            No filters yet. Add one to narrow the rows you want back.
          </div>
        )}

        {filters.map((filter) => (
          <div
            key={filter.id}
            className="grid gap-2 rounded-xl border border-zinc-800 bg-zinc-950 p-3 md:grid-cols-[1.2fr,1fr,1.4fr,auto]"
          >
            <select
              value={filter.field}
              onChange={(e) =>
                updateFilter(filter.id, { field: e.target.value })
              }
              className="rounded-lg border border-zinc-700 bg-zinc-800 p-2 text-sm text-white outline-none"
            >
              {entity.fields.map((field) => (
                <option key={field} value={field}>
                  {field}
                </option>
              ))}
            </select>

            <select
              value={filter.operator}
              onChange={(e) =>
                updateFilter(filter.id, {
                  operator: e.target.value as FilterOperator,
                })
              }
              className="rounded-lg border border-zinc-700 bg-zinc-800 p-2 text-sm text-white outline-none"
            >
              <option value="eq">equals</option>
              <option value="ne">not equals</option>
              <option value="gt">greater than</option>
              <option value="gte">greater or equal</option>
              <option value="lt">less than</option>
              <option value="lte">less or equal</option>
              <option value="like">contains / like</option>
            </select>

            <input
              value={filter.value}
              onChange={(e) =>
                updateFilter(filter.id, { value: e.target.value })
              }
              placeholder='Value, e.g. 3 or "%alex%"'
              className="rounded-lg border border-zinc-700 bg-zinc-800 p-2 text-sm outline-none placeholder:text-zinc-500"
            />

            <button
              type="button"
              onClick={() => removeFilter(filter.id)}
              className="rounded-lg border border-zinc-700 px-3 py-2 text-sm text-zinc-300"
            >
              Remove
            </button>
          </div>
        ))}
      </div>

      <div>
        <label className="text-sm text-zinc-400">Limit</label>
        <input
          value={limit}
          onChange={(e) => setLimit(e.target.value)}
          placeholder="Optional row limit"
          className="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-800 p-2 text-white outline-none placeholder:text-zinc-500"
        />
      </div>

      <div>
        <p className="mb-2 text-sm text-zinc-400">Generated Query</p>
        <pre className="overflow-auto rounded-xl border border-zinc-800 bg-black p-4 text-xs text-green-400">
          {JSON.stringify(query, null, 2)}
        </pre>
      </div>
    </div>
  )
}

function parseFilterValue(value: string): unknown {
  const trimmed = value.trim()

  if (trimmed === 'true') {
    return true
  }

  if (trimmed === 'false') {
    return false
  }

  if (trimmed === 'null') {
    return null
  }

  if (/^-?\d+(\.\d+)?$/.test(trimmed)) {
    return Number(trimmed)
  }

  if (
    (trimmed.startsWith('[') && trimmed.endsWith(']')) ||
    (trimmed.startsWith('{') && trimmed.endsWith('}'))
  ) {
    try {
      return JSON.parse(trimmed)
    } catch {
      return trimmed
    }
  }

  return trimmed
}
