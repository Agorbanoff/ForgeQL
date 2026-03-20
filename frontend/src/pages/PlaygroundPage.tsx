import { SchemaPanel } from '../components/SchemaPanel'
import { QueryBuilder } from '../components/QueryBuilder'
import { ResponsePanel } from '../components/ResponsePanel'

export default function PlaygroundPage() {
  return (
    <main className="mx-auto grid min-h-[calc(100vh-80px)] max-w-7xl grid-cols-12 gap-4 p-4">
      <aside className="col-span-3 rounded-xl border border-zinc-800 bg-zinc-900 p-4">
        <SchemaPanel />
      </aside>

      <section className="col-span-5 rounded-xl border border-zinc-800 bg-zinc-900 p-4">
        <QueryBuilder onRunQuery={() => {}} loading={false} />
      </section>

      <section className="col-span-4 rounded-xl border border-zinc-800 bg-zinc-900 p-4">
        <ResponsePanel data={null} error={null} loading={false} />
      </section>
    </main>
  )
}