import { useEffect, useState } from 'react'
import { SchemaPanel } from '../components/SchemaPanel'
import { QueryBuilder } from '../components/QueryBuilder'
import { ResponsePanel } from '../components/ResponsePanel'
import { runQuery } from '../api/queryApi'
import {
  getStoredDatasourceDetails,
  type StoredDataSourceSummary,
} from '../lib/appState'

export default function PlaygroundPage() {
  const [data, setData] = useState<any>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [activeDataSource, setActiveDataSource] =
    useState<StoredDataSourceSummary | null>(getStoredDatasourceDetails())
  const [connectionStatus, setConnectionStatus] = useState<string | null>(null)

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
    <main className="mx-auto grid min-h-[calc(100vh-80px)] max-w-7xl grid-cols-12 gap-4 p-4">
      <aside className="col-span-3 rounded-xl border border-zinc-800 bg-zinc-900 p-4">
        <SchemaPanel connection={activeDataSource} />
      </aside>

      <section className="col-span-5 rounded-xl border border-zinc-800 bg-zinc-900 p-4">
        {connectionStatus && (
          <div className="mb-4 rounded-xl border border-amber-800 bg-amber-950/30 p-3 text-sm text-amber-300">
            {connectionStatus}
          </div>
        )}
        <QueryBuilder onRunQuery={handleRunQuery} loading={loading} />
      </section>

      <section className="col-span-4 rounded-xl border border-zinc-800 bg-zinc-900 p-4">
        <ResponsePanel data={data} error={error} loading={loading} />
      </section>
    </main>
  )
}
