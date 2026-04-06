import { useMemo, useState } from 'react'
import { schema } from '../data/schema'
import { useElegantAnimations } from '../hooks/useElegantAnimations'

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

export function QueryBuilder({ onRunQuery, loading }: QueryBuilderProps) {
  const entityNames = Object.keys(schema.entities) as EntityKey[]

  const [selectedEntity, setSelectedEntity] = useState<EntityKey>('users')
  const [selectedFields, setSelectedFields] = useState<string[]>([])
  const [filters, setFilters] = useState<FilterRow[]>([])
  const [limit, setLimit] = useState('')
  const rootRef = useElegantAnimations<HTMLDivElement>([
    selectedEntity,
    selectedFields.join(','),
    filters.length,
    limit,
    loading,
  ])

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

  const query = useMemo(() => {
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
  }, [filters, limit, selectedEntity, selectedFields])

  return (
    <div ref={rootRef} className="space-y-5">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">
            Query composer
          </p>
          <h2 className="display-title mt-3 text-[2.3rem] text-white">Build a request</h2>
          <p className="mt-3 text-sm leading-6 text-zinc-400">
            Select an entity, choose the fields, then tighten the result with filters and a limit.
          </p>
        </div>

        <button
          type="button"
          onClick={() => onRunQuery(query)}
          disabled={loading || selectedFields.length === 0}
          className="primary-button"
          data-pressable
          data-glow={
            !loading && selectedFields.length > 0 ? 'pulse' : undefined
          }
        >
          {loading ? 'Running...' : 'Run query'}
        </button>
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        <div className="surface-card p-5" data-animate="panel" data-tilt>
          <label className="text-xs uppercase tracking-[0.2em] text-zinc-500">
            Entity
          </label>
          <select
            value={selectedEntity}
            onChange={(e) => {
              setSelectedEntity(e.target.value as EntityKey)
              setSelectedFields([])
              setFilters([])
            }}
            className="input-shell mt-3"
          >
            {entityNames.map((entityName) => (
              <option key={entityName} value={entityName}>
                {entityName}
              </option>
            ))}
          </select>
        </div>

        <div className="surface-card p-5" data-animate="panel" data-tilt>
          <label className="text-xs uppercase tracking-[0.2em] text-zinc-500">
            Limit
          </label>
          <input
            value={limit}
            onChange={(e) => setLimit(e.target.value)}
            placeholder="Optional row limit"
            className="input-shell mt-3"
          />
        </div>
      </div>

      <div className="surface-card p-5" data-animate="panel" data-tilt>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="text-xs uppercase tracking-[0.2em] text-zinc-500">Fields</p>
            <p className="mt-2 text-sm text-zinc-400">
              {selectedFields.length} selected out of {entity.fields.length}
            </p>
          </div>

          <span className="rounded-full border border-white/8 bg-white/[0.03] px-3 py-1.5 text-xs text-zinc-300">
            Entity: {selectedEntity}
          </span>
        </div>

        <div className="mt-5 flex flex-wrap gap-3">
          {entity.fields.map((field) => {
            const active = selectedFields.includes(field)

            return (
              <button
                key={field}
                type="button"
                onClick={() => toggleField(field)}
                className={`rounded-full border px-4 py-2 text-sm transition ${
                  active
                    ? 'border-cyan-300/30 bg-cyan-300 text-slate-950 shadow-[0_12px_30px_rgba(82,215,255,0.22)]'
                    : 'border-white/8 bg-white/[0.03] text-zinc-200 hover:border-cyan-300/20 hover:bg-cyan-300/8'
                }`}
                data-pressable
              >
                {field}
              </button>
            )
          })}
        </div>
      </div>

      <div className="surface-card p-5" data-animate="panel" data-tilt>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="text-xs uppercase tracking-[0.2em] text-zinc-500">Filters</p>
            <p className="mt-2 text-sm text-zinc-400">
              Narrow the request with field operators and typed values.
            </p>
          </div>

          <button
            type="button"
            onClick={addFilter}
            className="secondary-button px-4 py-2"
            data-pressable
          >
            Add filter
          </button>
        </div>

        {filters.length === 0 && (
          <div className="mt-5 rounded-[22px] border border-white/8 bg-white/[0.02] p-4 text-sm text-zinc-500">
            No filters yet. Add one to narrow the rows you want back.
          </div>
        )}

        {filters.length > 0 && (
          <div className="mt-5 space-y-3">
            {filters.map((filter) => (
              <div
                key={filter.id}
                className="grid gap-3 rounded-[24px] border border-white/8 bg-white/[0.02] p-4 md:grid-cols-[1.1fr_0.95fr_1.4fr_auto]"
                data-animate="panel"
                data-tilt
              >
                <select
                  value={filter.field}
                  onChange={(e) =>
                    updateFilter(filter.id, { field: e.target.value })
                  }
                  className="input-shell"
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
                  className="input-shell"
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
                  className="input-shell"
                />

                <button
                  type="button"
                  onClick={() => removeFilter(filter.id)}
                  className="secondary-button px-4 py-2"
                  data-pressable
                >
                  Remove
                </button>
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="surface-card p-5" data-animate="panel" data-tilt>
        <div className="flex items-center justify-between gap-3">
          <div>
            <p className="text-xs uppercase tracking-[0.2em] text-zinc-500">
              Generated query
            </p>
            <p className="mt-2 text-sm text-zinc-400">
              This is the exact payload the client will submit.
            </p>
          </div>

          <span className="rounded-full border border-white/8 bg-white/[0.03] px-3 py-1.5 text-xs text-zinc-300">
            JSON preview
          </span>
        </div>

        <pre className="data-block mt-5">{JSON.stringify(query, null, 2)}</pre>
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
