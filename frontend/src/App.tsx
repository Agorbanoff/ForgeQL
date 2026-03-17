import { useState } from 'react'
import { SchemaPanel } from './components/SchemaPanel'
import { QueryBuilder } from './components/QueryBuilder'
import { ResponsePanel } from './components/ResponsePanel'
import { runQuery } from './api/queryApi'

export default function App() {
  const [responseData, setResponseData] = useState<unknown>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function handleRunQuery(payload: unknown) {
    try {
      setLoading(true)
      setError(null)

      const data = await runQuery(payload)
      setResponseData(data)
    } catch (err) {
      setResponseData(null)
      setError(err instanceof Error ? err.message : 'Unknown error')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-zinc-950 text-white">
      <header className="border-b border-zinc-800 px-6 py-4">
        <h1 className="text-2xl font-semibold">ForgeQL Playground</h1>
        <p className="mt-1 text-sm text-zinc-400">
          Build and test dynamic queries
        </p>
      </header>

      <main className="grid min-h-[calc(100vh-81px)] grid-cols-12 gap-4 p-4">
        <aside className="col-span-3 rounded-2xl border border-zinc-800 bg-zinc-900 p-4">
          <SchemaPanel />
        </aside>

        <section className="col-span-5 rounded-2xl border border-zinc-800 bg-zinc-900 p-4">
          <QueryBuilder onRunQuery={handleRunQuery} loading={loading} />
        </section>

        <section className="col-span-4 rounded-2xl border border-zinc-800 bg-zinc-900 p-4">
          <ResponsePanel
            data={responseData}
            error={error}
            loading={loading}
          />
        </section>
      </main>
    </div>
  )
}