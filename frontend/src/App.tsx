import { SchemaPanel } from './components/SchemaPanel'
import { QueryBuilder } from './components/QueryBuilder'

export default function App() {
  return (
    <div className="min-h-screen bg-zinc-950 text-white">
      <header className="border-b border-zinc-800 px-6 py-4">
        <h1 className="text-2xl font-semibold">ForgeQL Playground</h1>
        <p className="mt-1 text-sm text-zinc-400">
          Build and test dynamic queries
        </p>
      </header>

      <main className="grid min-h-[calc(100vh-81px)] grid-cols-12 gap-4 p-4">
        {/* LEFT - Schema */}
        <aside className="col-span-3 rounded-2xl border border-zinc-800 bg-zinc-900 p-4">
          <SchemaPanel />
        </aside>

        {/* MIDDLE - Query Builder */}
        <section className="col-span-5 rounded-2xl border border-zinc-800 bg-zinc-900 p-4">
          <QueryBuilder />
        </section>

        {/* RIGHT - Response */}
        <section className="col-span-4 rounded-2xl border border-zinc-800 bg-zinc-900 p-4">
          <h2 className="mb-3 text-lg font-semibold">Response</h2>
          <p className="text-sm text-zinc-400">
            Next: backend response preview.
          </p>
        </section>
      </main>
    </div>
  )
}