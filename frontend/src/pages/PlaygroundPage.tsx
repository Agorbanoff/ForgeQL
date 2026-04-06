import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { runQuery } from '../api/queryApi'
import { QueryBuilder } from '../components/QueryBuilder'
import { ResponsePanel } from '../components/ResponsePanel'
import { SchemaPanel } from '../components/SchemaPanel'
import { schema } from '../data/schema'
import { useElegantAnimations } from '../hooks/useElegantAnimations'
import {
  getStoredDatasourceDetails,
  type StoredDataSourceSummary,
} from '../lib/appState'

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
  const [data, setData] = useState<unknown>(null)
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
        `Preview mode is active for ${saved.databaseName} on ${saved.host}.`
      )
    } else {
      setConnectionStatus('No saved datasource found. Return to setup and add one first.')
    }
  }, [])

  async function handleRunQuery(payload: unknown) {
    try {
      setLoading(true)
      setError(null)

      const result = await runQuery(payload)
      setData(result)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not generate demo results.')
      setData(null)
    } finally {
      setLoading(false)
    }
  }

  return (
    <main ref={rootRef} className="page-shell py-6 sm:py-8" data-animate="scene">
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
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <span className="section-badge" data-animate="chip">
                Step 3 of 3
              </span>
              <h1 className="display-title mt-6 text-[2.9rem] text-white sm:text-[3.8rem]">
                Query workspace
              </h1>
              <p className="display-copy mt-4 max-w-2xl text-sm sm:text-base">
                {activeDataSource
                  ? `${activeDataSource.name} is connected. Build a request, run it, and inspect the response.`
                  : 'A datasource is still required before the workspace can be used.'}
              </p>
            </div>

            <Link
              to="/connection-request"
              className="secondary-button"
              data-pressable
            >
              Change datasource
            </Link>
          </div>

          {connectionStatus && (
            <div className="mt-6 rounded-[20px] border border-amber-300/15 bg-amber-300/8 p-4 text-sm text-amber-100">
              {connectionStatus}
            </div>
          )}

          <div className="mt-6 grid gap-3 sm:grid-cols-3">
            {workspaceStats.map((stat) => (
              <div
                key={stat.label}
                className="rounded-[22px] border border-white/8 bg-white/[0.03] px-4 py-4"
                data-animate="panel"
              >
                <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">
                  {stat.label}
                </p>
                <p
                  className="stat-value mt-3 text-4xl text-white"
                  {...(/^\d+$/.test(stat.value) ? { 'data-count-up': stat.value } : {})}
                >
                  {stat.value}
                </p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <div className="mt-6 grid gap-6 xl:grid-cols-[300px_minmax(0,1fr)_360px]">
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
