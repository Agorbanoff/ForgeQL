import { schema } from '../data/schema'
import type { StoredDataSourceSummary } from '../lib/appState'
import { useElegantAnimations } from '../hooks/useElegantAnimations'

type SchemaPanelProps = {
  connection?: StoredDataSourceSummary | null
}

export function SchemaPanel({ connection }: SchemaPanelProps) {
  const rootRef = useElegantAnimations<HTMLDivElement>([connection?.name])
  const entities = Object.entries(schema.entities)

  return (
    <div ref={rootRef} className="space-y-5">
      <div>
        <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">Schema map</p>
        <h2 className="display-title mt-3 text-[2.2rem] text-white">Explore entities</h2>
        <p className="mt-3 text-sm leading-6 text-zinc-400">
          Review the shape of the demo schema before composing the JSON request.
        </p>
      </div>

      {connection && (
        <div className="surface-card p-5" data-animate="panel" data-pressable>
          <p className="text-xs uppercase tracking-[0.2em] text-cyan-200/80">
            Connected DB
          </p>
          <p className="mt-3 text-base font-semibold text-cyan-100">{connection.name}</p>
          <p className="mt-1 text-sm text-zinc-300">
            {connection.dbType} on {connection.host}:{connection.port}
          </p>
          <p className="text-sm text-zinc-500">
            {connection.databaseName}
            {connection.schemaName ? ` / ${connection.schemaName}` : ''}
          </p>
        </div>
      )}

      <div className="grid gap-4">
        {entities.map(([entityName, entity]) => {
          const relations = Object.entries(entity.relations ?? {})

          return (
            <article
              key={entityName}
              className="surface-card p-5"
              data-animate="panel"
              data-pressable
            >
              <div className="flex items-start justify-between gap-3">
                <div>
                  <h3 className="text-lg font-semibold text-cyan-200">{entityName}</h3>
                  <p className="mt-1 text-xs uppercase tracking-[0.2em] text-zinc-500">
                    table / {entity.table}
                  </p>
                </div>

                <div className="rounded-full border border-white/8 bg-white/[0.03] px-3 py-1 text-xs text-zinc-300">
                  {entity.fields.length} fields
                </div>
              </div>

              <div className="mt-5">
                <p className="mb-2 text-xs uppercase tracking-[0.2em] text-zinc-500">Fields</p>
                <div className="flex flex-wrap gap-2">
                  {entity.fields.map((field) => (
                    <span
                      key={field}
                      className="rounded-full border border-white/8 bg-white/[0.03] px-3 py-1.5 text-xs text-zinc-200"
                    >
                      {field}
                    </span>
                  ))}
                </div>
              </div>

              <div className="mt-5">
                <p className="mb-2 text-xs uppercase tracking-[0.2em] text-zinc-500">
                  Relations
                </p>

                {relations.length === 0 ? (
                  <div className="rounded-[18px] border border-white/8 bg-white/[0.02] px-4 py-3 text-sm text-zinc-500">
                    No relations configured.
                  </div>
                ) : (
                  <div className="space-y-3">
                    {relations.map(([relationName, relation]) => (
                      <div
                        key={relationName}
                        className="rounded-[20px] border border-white/8 bg-white/[0.02] px-4 py-3"
                      >
                        <p className="text-sm font-medium text-zinc-100">{relationName}</p>
                        <p className="mt-1 text-xs text-zinc-400">
                          {relation.type} {'->'} {relation.target}
                        </p>
                        <p className="text-xs text-zinc-500">
                          {relation.localKey} {'->'} {relation.foreignKey}
                        </p>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </article>
          )
        })}
      </div>
    </div>
  )
}
