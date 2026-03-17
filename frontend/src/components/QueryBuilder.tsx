import { useState } from 'react'
import { schema } from '../data/schema'

type EntityKey = keyof typeof schema.entities

export function QueryBuilder() {
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

  const query = {
    entity: selectedEntity,
    fields: selectedFields,
  }

  return (
    <div className="space-y-4">
      <h2 className="text-lg font-semibold">Query Builder</h2>

      <div>
        <label className="text-sm text-zinc-400">Entity</label>
        <select
          value={selectedEntity}
          onChange={(e) => {
            setSelectedEntity(e.target.value as EntityKey)
            setSelectedFields([])
          }}
          className="mt-1 w-full rounded-lg bg-zinc-800 p-2 text-white"
        >
          {Object.keys(schema.entities).map((entityName) => (
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
        <pre className="overflow-auto rounded-lg bg-black p-3 text-xs text-green-400">
          {JSON.stringify(query, null, 2)}
        </pre>
      </div>
    </div>
  )
}