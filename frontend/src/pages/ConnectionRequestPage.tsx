import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { saveDataSource } from '../api/dataSourceApi'
import { storeSavedDatasourceDetails } from '../lib/appState'
import { ApiRequestError } from '../api/http'

const DATABASE_OPTIONS = [
  { value: 'POSTGRESQL', label: 'PostgreSQL', defaultPort: '5432' },
  { value: 'MYSQL', label: 'MySQL', defaultPort: '3306' },
] as const

const SSL_OPTIONS = [
  { value: 'REQUIRE', label: 'Require' },
  { value: 'VERIFY_CA', label: 'Verify CA' },
  { value: 'VERIFY_FULL', label: 'Verify Full' },
] as const

type DatabaseOption = (typeof DATABASE_OPTIONS)[number]['value']
type SslOption = (typeof SSL_OPTIONS)[number]['value']

type ErrorDetails = {
  message: string
  status?: number
  path?: string
  timestamp?: string
}

export default function ConnectionRequestPage() {
  const navigate = useNavigate()

  const [form, setForm] = useState({
    name: '',
    dbType: 'POSTGRESQL' as DatabaseOption,
    host: '',
    port: '5432',
    databaseName: '',
    schemaName: 'public',
    username: '',
    encryptedPassword: '',
    sslEnabled: false,
    sslMode: 'REQUIRE' as SslOption,
  })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<ErrorDetails | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  function updateField<K extends keyof typeof form>(key: K, value: string) {
    setForm((prev) => ({
      ...prev,
      [key]: value,
    }))
  }

  function updateDbType(value: DatabaseOption) {
    const defaultPort =
      DATABASE_OPTIONS.find((option) => option.value === value)?.defaultPort ??
      '5432'

    setForm((prev) => ({
      ...prev,
      dbType: value,
      port: prev.port ? prev.port : defaultPort,
    }))
  }

  function updateSslEnabled(value: boolean) {
    setForm((prev) => ({
      ...prev,
      sslEnabled: value,
    }))
  }

  const payloadPreview = useMemo(() => {
    const trimmedName = form.name.trim()
    const trimmedSchema = form.schemaName.trim()

    return {
      name:
        trimmedName ||
        `${form.databaseName.trim() || form.host.trim() || 'SigmaQL'} connection`,
      dbType: form.dbType,
      host: form.host.trim(),
      port: Number(form.port),
      databaseName: form.databaseName.trim(),
      username: form.username.trim(),
      encryptedPassword: form.encryptedPassword,
      ...(trimmedSchema ? { schemaName: trimmedSchema } : {}),
      SslEnabled: form.sslEnabled,
      ...(form.sslEnabled ? { sslMode: form.sslMode } : {}),
    }
  }, [form])

  async function handleSave() {
    setError(null)
    setSuccess(null)

    if (!form.host.trim()) {
      setError({ message: 'Host is required.' })
      return
    }

    if (!form.port.trim() || Number.isNaN(Number(form.port))) {
      setError({ message: 'A valid numeric port is required.' })
      return
    }

    if (Number(form.port) < 1 || Number(form.port) > 65535) {
      setError({ message: 'Port must be between 1 and 65535.' })
      return
    }

    if (!form.databaseName.trim()) {
      setError({ message: 'Database name is required.' })
      return
    }

    if (!form.username.trim()) {
      setError({ message: 'Username is required.' })
      return
    }

    if (!form.encryptedPassword) {
      setError({ message: 'Password is required.' })
      return
    }

    storeSavedDatasourceDetails({
      name: payloadPreview.name,
      dbType: payloadPreview.dbType,
      host: payloadPreview.host,
      port: payloadPreview.port,
      databaseName: payloadPreview.databaseName,
      username: payloadPreview.username,
      schemaName: payloadPreview.schemaName ?? null,
      encryptedPassword: payloadPreview.encryptedPassword,
      sslEnabled: form.sslEnabled,
      sslMode: form.sslEnabled ? form.sslMode : null,
    })

    try {
      setLoading(true)
      await saveDataSource(payloadPreview)
      setSuccess('Connection details saved. Opening the playground...')
    } catch (err) {
      if (err instanceof ApiRequestError) {
        setSuccess(
          'Connection details saved locally for demo mode. Opening the playground...'
        )
      } else {
        setSuccess(
          'Connection details saved locally for demo mode. Opening the playground...'
        )
      }

      setError(null)
    } finally {
      setLoading(false)
    }

    setTimeout(() => {
      navigate('/playground')
    }, 500)
  }

  return (
    <main className="flex min-h-[calc(100vh-80px)] justify-center px-4 py-8">
      <div className="w-full max-w-3xl rounded-xl border border-zinc-800 bg-zinc-900 p-6">
        <div className="mb-6">
          <h2 className="text-3xl font-semibold">Database Connection Request</h2>
          <p className="mt-2 text-sm text-zinc-400">
            Save the database details you want SigmaQL to query from the playground.
          </p>
        </div>

        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <div>
            <label className="mb-2 block text-sm text-zinc-300">
              Connection name
            </label>
            <input
              value={form.name}
              onChange={(e) => updateField('name', e.target.value)}
              placeholder="My app database"
              className="w-full rounded-lg border border-zinc-700 bg-zinc-800 p-3 outline-none placeholder:text-zinc-500"
            />
          </div>

          <div>
            <label className="mb-2 block text-sm text-zinc-300">
              Database type
            </label>
            <select
              value={form.dbType}
              onChange={(e) => updateDbType(e.target.value as DatabaseOption)}
              className="w-full rounded-lg border border-zinc-700 bg-zinc-800 p-3 outline-none"
            >
              {DATABASE_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
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
              value={form.schemaName}
              onChange={(e) => updateField('schemaName', e.target.value)}
              placeholder="public"
              className="w-full rounded-lg border border-zinc-700 bg-zinc-800 p-3 outline-none placeholder:text-zinc-500"
            />
          </div>

          <div>
            <label className="mb-2 block text-sm text-zinc-300">
              Username
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
              value={form.encryptedPassword}
              onChange={(e) => updateField('encryptedPassword', e.target.value)}
              placeholder="Enter password"
              className="w-full rounded-lg border border-zinc-700 bg-zinc-800 p-3 outline-none placeholder:text-zinc-500"
            />
          </div>

          <div>
            <label className="mb-2 block text-sm text-zinc-300">
              Is SSL required?
            </label>
            <select
              value={form.sslEnabled ? 'yes' : 'no'}
              onChange={(e) => updateSslEnabled(e.target.value === 'yes')}
              className="w-full rounded-lg border border-zinc-700 bg-zinc-800 p-3 outline-none"
            >
              <option value="no">No</option>
              <option value="yes">Yes</option>
            </select>
          </div>

          <div>
            <label className="mb-2 block text-sm text-zinc-300">SSL type</label>
            <select
              value={form.sslMode}
              onChange={(e) => updateField('sslMode', e.target.value as SslOption)}
              className="w-full rounded-lg border border-zinc-700 bg-zinc-800 p-3 outline-none placeholder:text-zinc-500"
              disabled={!form.sslEnabled}
            >
              {SSL_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </div>
        </div>

        {error && (
          <div className="mt-6 rounded-lg border border-red-800 bg-red-950/40 p-4 text-sm text-red-300">
            <p className="font-medium">{error.message}</p>
            {error.status && (
              <p className="mt-2 text-xs text-red-200/80">Status: {error.status}</p>
            )}
            {error.path && (
              <p className="text-xs text-red-200/80">Path: {error.path}</p>
            )}
            {error.timestamp && (
              <p className="text-xs text-red-200/80">
                Time: {new Date(error.timestamp).toLocaleString()}
              </p>
            )}
            <p className="mt-3 text-xs text-red-200/80">
              Check the host, port, database name, username, and SSL settings if this keeps happening.
            </p>
          </div>
        )}

        {success && (
          <div className="mt-6 rounded-lg border border-emerald-800 bg-emerald-950/40 p-4 text-sm text-emerald-300">
            {success}
          </div>
        )}

        <div className="mt-6 rounded-lg border border-zinc-800 bg-zinc-950 p-4">
          <p className="mb-2 text-sm text-zinc-400">Preview</p>
          <pre className="overflow-auto text-xs text-green-400">
            {JSON.stringify(payloadPreview, null, 2)}
          </pre>
        </div>

        <div className="mt-6 flex justify-end">
          <button
            type="button"
            onClick={handleSave}
            disabled={loading}
            className="rounded-lg bg-cyan-500 px-5 py-3 font-medium text-black disabled:cursor-not-allowed disabled:opacity-60"
          >
            {loading ? 'Saving...' : 'Save details'}
          </button>
        </div>
      </div>
    </main>
  )
}
