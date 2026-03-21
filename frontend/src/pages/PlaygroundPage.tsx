import { SchemaPanel } from '../components/SchemaPanel'
import { QueryBuilder } from '../components/QueryBuilder'
import { ResponsePanel } from '../components/ResponsePanel'
import { runQuery } from '../api/queryApi'
import { useState } from 'react'

export default function PlaygroundPage() {
  const [data, setData] = useState<any>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function handleRunQuery(payload: unknown) {
    try {
      setLoading(true)
      setError(null)

      const result = await runQuery(payload)
      setData(result)
    } catch (err: any) {
      setError(err.message || 'Error')
      setData(null)
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="mx-auto grid min-h-[calc(100vh-80px)] max-w-7xl grid-cols-12 gap-4 p-4">
      <aside className="col-span-3 rounded-xl border border-zinc-800 bg-zinc-900 p-4">
        <SchemaPanel />
      </aside>

      <section className="col-span-5 rounded-xl border border-zinc-800 bg-zinc-900 p-4">
        <QueryBuilder onRunQuery={handleRunQuery} loading={loading} />
      </section>

      <section className="col-span-4 rounded-xl border border-zinc-800 bg-zinc-900 p-4">
        <ResponsePanel data={data} error={error} loading={loading} />
      </section>
    </main>
  )
}