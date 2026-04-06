import { useMemo, useState } from 'react'
import { schema } from '../data/schema'
import { useElegantAnimations } from '../hooks/useElegantAnimations'
import { AnimatedSelect } from './AnimatedSelect'

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

const operatorOptions = [
  { value: 'eq', label: 'Equals' },
  { value: 'ne', label: 'Not equals' },
  { value: 'gt', label: 'Greater than' },
  { value: 'gte', label: 'Greater or equal' },
  { value: 'lt', label: 'Less than' },
  { value: 'lte', label: 'Less or equal' },
  { value: 'like', label: 'Contains' },
] as const

export function QueryBuilder({ onRunQuery, loading }: QueryBuilderProps) {
  const entityNames = Object.keys(schema.entities) as EntityKey[]
  const entityOptions = entityNames.map((entityName) => ({
    value: entityName,
    label: entityName,
  }))

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
  const fieldOptions = entity.fields.map((field) => ({
    value: field,
    label: field,
  }))

  function toggleField(field: string) {
    setSelectedFields((previous) =>
      previous.includes(field)
        ? previous.filter((item) => item !== field)
        : [...previous, field]
    )
  }

  function addFilter() {
    setFilters((previous) => [
      ...previous,
      {
        id: Date.now() + previous.length,
        field: entity.fields[0] ?? '',
        operator: 'eq',
        value: '',
      },
    ])
  }

  function updateFilter(id: number, patch: Partial<FilterRow>) {
    setFilters((previous) =>
      previous.map((filter) =>
        filter.id === id ? { ...filter, ...patch } : filter
      )
    )
  }

  function removeFilter(id: number) {
    setFilters((previous) => previous.filter((filter) => filter.id !== id))
  }

  const query = useMemo(() => {
    const filterPayload = filters.reduce<Record<string, Record<string, unknown>>>(
      (accumulator, filter) => {
        if (!filter.field || !filter.value.trim()) {
          return accumulator
        }

        accumulator[filter.field] = {
          ...(accumulator[filter.field] ?? {}),
          [filter.operator]: parseFilterValue(filter.value),
        }

        return accumulator
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
    <div ref={rootRef} className="space-y-4">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">
            Query builder
          </p>
          <h2 className="display-title mt-3 text-[2.1rem] text-white">
            Build the request
          </h2>
        </div>

        <button
          type="button"
          onClick={() => onRunQuery(query)}
          disabled={loading || selectedFields.length === 0}
          className="primary-button"
          data-pressable
          data-glow={!loading && selectedFields.length > 0 ? 'pulse' : undefined}
        >
          {loading ? 'Running...' : 'Run query'}
        </button>
      </div>

      <div className="grid gap-4 md:grid-cols-[minmax(0,1fr)_180px]">
        <div className="surface-card p-5" data-animate="panel" data-tilt>
          <label className="text-xs uppercase tracking-[0.2em] text-zinc-500">
            Entity
          </label>
          <div className="mt-3">
            <AnimatedSelect
              value={selectedEntity}
              onChange={(value) => {
                setSelectedEntity(value as EntityKey)
                setSelectedFields([])
                setFilters([])
              }}
              options={entityOptions}
              ariaLabel="Entity"
            />
          </div>
        </div>

        <div className="surface-card p-5" data-animate="panel" data-tilt>
          <label className="text-xs uppercase tracking-[0.2em] text-zinc-500">
            Limit
          </label>
          <input
            value={limit}
            onChange={(event) => setLimit(event.target.value)}
            placeholder="Optional"
            className="input-shell mt-3"
          />
        </div>
      </div>

      <div className="surface-card p-5" data-animate="panel" data-tilt>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <p className="text-xs uppercase tracking-[0.2em] text-zinc-500">Fields</p>
          <span className="rounded-full border border-white/8 bg-white/[0.03] px-3 py-1.5 text-xs text-zinc-300">
            {selectedFields.length} / {entity.fields.length} selected
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
          <p className="text-xs uppercase tracking-[0.2em] text-zinc-500">Filters</p>

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
            No filters yet.
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
                <AnimatedSelect
                  value={filter.field}
                  onChange={(value) => updateFilter(filter.id, { field: value })}
                  options={fieldOptions}
                  ariaLabel="Filter field"
                />

                <AnimatedSelect
                  value={filter.operator}
                  onChange={(value) =>
                    updateFilter(filter.id, {
                      operator: value as FilterOperator,
                    })
                  }
                  options={operatorOptions}
                  ariaLabel="Filter operator"
                />

                <input
                  value={filter.value}
                  onChange={(event) =>
                    updateFilter(filter.id, { value: event.target.value })
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
          <p className="text-xs uppercase tracking-[0.2em] text-zinc-500">
            Generated query
          </p>

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
