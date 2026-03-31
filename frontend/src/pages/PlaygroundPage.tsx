import { useEffect, useState } from 'react'
import { SchemaPanel } from '../components/SchemaPanel'
import { QueryBuilder } from '../components/QueryBuilder'
import { ResponsePanel } from '../components/ResponsePanel'
import { runQuery } from '../api/queryApi'
import {
  getStoredDatasourceDetails,
  type StoredDataSourceSummary,
} from '../lib/appState'
import { schema } from '../data/schema'
import { useElegantAnimations } from '../hooks/useElegantAnimations'

const workspaceStats = [
  { label: 'Entities', value: String(Object.keys(schema.entities).length) },
  {
    label: 'Relations',
    value: String(
      Object.values(schema.entities).reduce(
        (total, entity) => total + Object.keys(entity.relations ?? {}).length,
        0
      )
    ),
  },
  { label: 'Flow', value: 'Live demo' },
]

export default function PlaygroundPage() {
  const [data, setData] = useState<any>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [activeDataSource, setActiveDataSource] =
    useState<StoredDataSourceSummary | null>(getStoredDatasourceDetails())
  const [connectionStatus, setConnectionStatus] = useState<string | null>(null)
  const rootRef = useElegantAnimations<HTMLDivElement>([activeDataSource?.name])

  useEffect(() => {
    const saved = getStoredDatasourceDetails()
    if (saved) {
      setActiveDataSource(saved)
      setConnectionStatus(
        `Demo mode is on. Showing preview data for ${saved.databaseName} on ${saved.host}.`
      )
    } else {
      setConnectionStatus('No saved datasource found. Go back and save one first.')
    }
  }, [])

  async function handleRunQuery(payload: unknown) {
    try {
      setLoading(true)
      setError(null)

      const result = await runQuery(payload)
      setData(result)
    } catch (err: any) {
      setError(err.message || 'Could not generate demo results.')
      setData(null)
    } finally {
      setLoading(false)
    }
  }

  return (
    <main ref={rootRef} className="page-shell py-6 sm:py-8">
      <div className="grid gap-6 xl:grid-cols-[1.15fr_0.85fr]">
        <section className="surface-panel px-6 py-7 sm:px-8 sm:py-8 lg:px-10" data-animate="hero">
          <div
            className="absolute -left-8 top-10 h-32 w-32 rounded-full bg-[radial-gradient(circle,_rgba(255,167,109,0.48),_transparent_72%)] blur-3xl"
            data-float="slow"
            data-glow="pulse"
          />
          <div
            className="absolute right-6 top-6 h-28 w-28 rounded-full bg-[radial-gradient(circle,_rgba(82,215,255,0.54),_transparent_72%)] blur-3xl"
            data-float="medium"
          />

          <div className="relative z-10">
            <span className="section-badge" data-animate="chip">
              Live Workspace
            </span>
            <h1 className="display-title mt-7 max-w-4xl text-[3rem] text-white sm:text-[4rem] lg:text-[4.8rem]">
              A smoother place to shape and run SigmaQL queries.
            </h1>
            <p className="display-copy mt-5 max-w-2xl text-sm sm:text-base">
              The workspace now carries the same darker editorial feel as the rest of the site:
              glassy panels, softly glowing accents, and click animations that make the builder feel more refined.
            </p>

            {connectionStatus && (
              <div className="mt-7 rounded-[24px] border border-amber-300/15 bg-amber-300/8 p-4 text-sm text-amber-100">
                {connectionStatus}
              </div>
            )}

            <div className="mt-7 flex flex-wrap gap-3">
              {[
                'Schema-aware selection',
                'Structured filters',
                'Readable JSON output',
              ].map((item) => (
                <span key={item} className="small-chip" data-animate="chip">
                  <span className="h-1.5 w-1.5 rounded-full bg-cyan-300" />
                  {item}
                </span>
              ))}
            </div>
          </div>
        </section>

        <section className="grid gap-4" data-animate="panel">
          <div className="surface-panel px-6 py-7">
            <div className="relative z-10">
              <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">
                Active source
              </p>
              <p className="mt-3 text-2xl font-semibold text-white">
                {activeDataSource?.name ?? 'No datasource selected'}
              </p>
                <p className="mt-2 text-sm text-zinc-400">
                  {activeDataSource
                    ? `${activeDataSource.dbType} / ${activeDataSource.host}:${activeDataSource.port}`
                    : 'Return to the datasource intake step to configure one.'}
                </p>
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-3 xl:grid-cols-1">
            {workspaceStats.map((stat) => (
              <div key={stat.label} className="surface-panel px-6 py-6" data-pressable>
                <div className="relative z-10">
                  <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">
                    {stat.label}
                  </p>
                  <p className="stat-value mt-3 text-5xl text-white">{stat.value}</p>
                </div>
              </div>
            ))}
          </div>
        </section>
      </div>

      <div className="mt-6 grid gap-6 xl:grid-cols-[320px_minmax(0,1fr)_380px]">
        <aside className="surface-panel px-5 py-6 sm:px-6" data-animate="panel">
          <SchemaPanel connection={activeDataSource} />
        </aside>

        <section className="surface-panel px-5 py-6 sm:px-6" data-animate="panel">
          <QueryBuilder onRunQuery={handleRunQuery} loading={loading} />
        </section>

        <section className="surface-panel px-5 py-6 sm:px-6" data-animate="panel">
          <ResponsePanel data={data} error={error} loading={loading} />
        </section>
      </div>
    </main>
  )
}
