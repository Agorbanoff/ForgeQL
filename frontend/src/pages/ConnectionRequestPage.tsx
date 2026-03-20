import { useState } from 'react'

export default function ConnectionRequestPage() {
  const [form, setForm] = useState({
    databaseType: 'PostgreSQL',
    host: '',
    port: '',
    databaseName: '',
    schema: '',
    username: '',
    password: '',
    sslRequired: 'no',
    sslType: '',
    networkAccess: '',
  })

  function updateField<K extends keyof typeof form>(key: K, value: string) {
    setForm((prev) => ({
      ...prev,
      [key]: value,
    }))
  }

  return (
    <main className="flex min-h-[calc(100vh-80px)] justify-center px-4 py-8">
      <div className="w-full max-w-3xl rounded-xl border border-zinc-800 bg-zinc-900 p-6">
        <div className="mb-6">
          <h2 className="text-3xl font-semibold">Database Connection Request</h2>
          <p className="mt-2 text-sm text-zinc-400">
            Fill in the database access details required for integration.
          </p>
        </div>

        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <div>
            <label className="mb-2 block text-sm text-zinc-300">
              Database type
            </label>
            <select
              value={form.databaseType}
              onChange={(e) => updateField('databaseType', e.target.value)}
              className="w-full rounded-lg border border-zinc-700 bg-zinc-800 p-3 outline-none"
            >
              <option>PostgreSQL</option>
              <option>MySQL</option>
              <option>MariaDB</option>
              <option>SQL Server</option>
              <option>Oracle</option>
              <option>Other</option>
            </select>
          </div>

          <div>
            <label className="mb-2 block text-sm text-zinc-300">
              Host (IP or domain)
            </label>
            <input
              value={form.host}
              onChange={(e) => updateField('host', e.target.value)}
              placeholder="db.example.com or 192.168.1.10"
              className="w-full rounded-lg border border-zinc-700 bg-zinc-800 p-3 outline-none placeholder:text-zinc-500"
            />
          </div>

          <div>
            <label className="mb-2 block text-sm text-zinc-300">Port</label>
            <input
              value={form.port}
              onChange={(e) => updateField('port', e.target.value)}
              placeholder="5432"
              className="w-full rounded-lg border border-zinc-700 bg-zinc-800 p-3 outline-none placeholder:text-zinc-500"
            />
          </div>

          <div>
            <label className="mb-2 block text-sm text-zinc-300">
              Database name
            </label>
            <input
              value={form.databaseName}
              onChange={(e) => updateField('databaseName', e.target.value)}
              placeholder="my_database"
              className="w-full rounded-lg border border-zinc-700 bg-zinc-800 p-3 outline-none placeholder:text-zinc-500"
            />
          </div>

          <div>
            <label className="mb-2 block text-sm text-zinc-300">
              Schema (optional)
            </label>
            <input
              value={form.schema}
              onChange={(e) => updateField('schema', e.target.value)}
              placeholder="public"
              className="w-full rounded-lg border border-zinc-700 bg-zinc-800 p-3 outline-none placeholder:text-zinc-500"
            />
          </div>

          <div>
            <label className="mb-2 block text-sm text-zinc-300">
              Username (read-only)
            </label>
            <input
              value={form.username}
              onChange={(e) => updateField('username', e.target.value)}
              placeholder="readonly_user"
              className="w-full rounded-lg border border-zinc-700 bg-zinc-800 p-3 outline-none placeholder:text-zinc-500"
            />
          </div>

          <div className="md:col-span-2">
            <label className="mb-2 block text-sm text-zinc-300">Password</label>
            <input
              type="password"
              value={form.password}
              onChange={(e) => updateField('password', e.target.value)}
              placeholder="Enter password"
              className="w-full rounded-lg border border-zinc-700 bg-zinc-800 p-3 outline-none placeholder:text-zinc-500"
            />
          </div>

          <div>
            <label className="mb-2 block text-sm text-zinc-300">
              Is SSL required?
            </label>
            <select
              value={form.sslRequired}
              onChange={(e) => updateField('sslRequired', e.target.value)}
              className="w-full rounded-lg border border-zinc-700 bg-zinc-800 p-3 outline-none"
            >
              <option value="no">No</option>
              <option value="yes">Yes</option>
            </select>
          </div>

          <div>
            <label className="mb-2 block text-sm text-zinc-300">SSL type</label>
            <input
              value={form.sslType}
              onChange={(e) => updateField('sslType', e.target.value)}
              placeholder="require / verify-ca / verify-full"
              className="w-full rounded-lg border border-zinc-700 bg-zinc-800 p-3 outline-none placeholder:text-zinc-500"
              disabled={form.sslRequired === 'no'}
            />
          </div>

          <div className="md:col-span-2">
            <label className="mb-2 block text-sm text-zinc-300">
              Network access details
            </label>
            <textarea
              value={form.networkAccess}
              onChange={(e) => updateField('networkAccess', e.target.value)}
              placeholder="Should our server IP be allowlisted, or is access provided through VPN?"
              rows={5}
              className="w-full rounded-lg border border-zinc-700 bg-zinc-800 p-3 outline-none placeholder:text-zinc-500"
            />
          </div>
        </div>

        <div className="mt-6 rounded-lg border border-zinc-800 bg-zinc-950 p-4">
          <p className="mb-2 text-sm text-zinc-400">Preview</p>
          <pre className="overflow-auto text-xs text-green-400">
            {JSON.stringify(form, null, 2)}
          </pre>
        </div>

        <div className="mt-6 flex justify-end">
          <button
            type="button"
            className="rounded-lg bg-cyan-500 px-5 py-3 font-medium text-black"
          >
            Save details
          </button>
        </div>
      </div>
    </main>
  )
}