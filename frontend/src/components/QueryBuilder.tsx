import { useMemo, useState } from 'react'
import { schema } from '../data/schema'

type EntityKey = keyof typeof schema.entities

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

  const entity = schema.entities[selectedEntity]

  function toggleField(field: string) {
    setSelectedFields((prev) =>
      prev.includes(field)
        ? prev.filter((f) => f !== field)
        : [...prev, field]
    )
  }

  const query = useMemo(
    () => ({
      entity: selectedEntity,
      fields: selectedFields,
    }),
    [selectedEntity, selectedFields]
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

      <div>
        <p className="mb-2 text-sm text-zinc-400">Generated Query</p>
        <pre className="overflow-auto rounded-xl border border-zinc-800 bg-black p-4 text-xs text-green-400">
          {JSON.stringify(query, null, 2)}
        </pre>
      </div>
    </div>
  )
}