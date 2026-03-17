import { schema } from '../data/schema'

export function SchemaPanel() {
  const entities = Object.entries(schema.entities)

  return (
    <div className="space-y-4">
      <h2 className="text-lg font-semibold">Schema</h2>

      {entities.map(([entityName, entity]) => {
        const relations = Object.entries(entity.relations ?? {})

        return (
          <div
            key={entityName}
            className="rounded-2xl border border-zinc-800 bg-zinc-950 p-4"
          >
            <div className="mb-3">
              <h3 className="text-base font-semibold text-cyan-400">
                {entityName}
              </h3>
              <p className="text-xs text-zinc-500">table: {entity.table}</p>
            </div>

            <div className="mb-4">
              <p className="mb-2 text-sm font-medium text-zinc-300">Fields</p>
              <div className="flex flex-wrap gap-2">
                {entity.fields.map((field) => (
                  <span
                    key={field}
                    className="rounded-md border border-zinc-700 px-2 py-1 text-xs text-zinc-200"
                  >
                    {field}
                  </span>
                ))}
              </div>
            </div>

            <div>
              <p className="mb-2 text-sm font-medium text-zinc-300">Relations</p>

              {relations.length === 0 ? (
                <p className="text-xs text-zinc-500">No relations</p>
              ) : (
                <div className="space-y-2">
                  {relations.map(([relationName, relation]) => (
                    <div
                      key={relationName}
                      className="rounded-xl border border-zinc-800 px-3 py-2"
                    >
                      <p className="text-sm text-zinc-200">{relationName}</p>
                      <p className="text-xs text-zinc-500">
                        {relation.type} → {relation.target}
                      </p>
                      <p className="text-xs text-zinc-600">
                        {relation.localKey} → {relation.foreignKey}
                      </p>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )
      })}
    </div>
  )
}