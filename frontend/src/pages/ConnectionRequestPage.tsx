import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { saveDataSource } from '../api/dataSourceApi'
import { ApiRequestError } from '../api/http'
import { AnimatedSelect } from '../components/AnimatedSelect'
import { useElegantAnimations } from '../hooks/useElegantAnimations'
import { storeSavedDatasourceDetails } from '../lib/appState'

const DATABASE_OPTIONS = [
  { value: 'POSTGRESQL', label: 'PostgreSQL', defaultPort: '5432' },
  { value: 'MYSQL', label: 'MySQL', defaultPort: '3306' },
] as const

const SSL_OPTIONS = [
  { value: 'REQUIRE', label: 'Require' },
  { value: 'VERIFY_CA', label: 'Verify CA' },
  { value: 'VERIFY_FULL', label: 'Verify Full' },
] as const

const SSL_REQUIRED_OPTIONS = [
  { value: 'no', label: 'Not required' },
  { value: 'yes', label: 'Required' },
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
  const rootRef = useElegantAnimations<HTMLDivElement>([])

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

  function updateField<K extends keyof typeof form>(key: K, value: (typeof form)[K]) {
    setForm((previous) => ({
      ...previous,
      [key]: value,
    }))
  }

  function updateDbType(value: DatabaseOption) {
    const nextDefaultPort =
      DATABASE_OPTIONS.find((option) => option.value === value)?.defaultPort ?? '5432'

    setForm((previous) => ({
      ...previous,
      dbType: value,
      port:
        !previous.port ||
        DATABASE_OPTIONS.some(
          (option) =>
            option.value === previous.dbType && option.defaultPort === previous.port
        )
          ? nextDefaultPort
          : previous.port,
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
      setSuccess('Connection details saved. Opening the workspace...')
    } catch (err) {
      if (err instanceof ApiRequestError) {
        setSuccess('Saved locally for demo mode. Opening the workspace...')
      } else {
        setSuccess('Saved locally for demo mode. Opening the workspace...')
      }

      setError(null)
    } finally {
      setLoading(false)
    }

    setTimeout(() => {
      navigate('/playground', { replace: true })
    }, 500)
  }

  return (
    <main ref={rootRef} className="page-shell py-6 sm:py-8" data-animate="scene">
      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_360px]">
        <section className="surface-panel px-6 py-7 sm:px-8 sm:py-8 lg:px-10" data-animate="hero">
          <div
            className="absolute left-[-4rem] top-10 h-32 w-32 rounded-full bg-[radial-gradient(circle,_rgba(255,171,115,0.56),_transparent_72%)] blur-3xl"
            data-float="slow"
            data-glow="pulse"
          />
          <div
            className="absolute right-6 top-6 h-28 w-28 rounded-full bg-[radial-gradient(circle,_rgba(82,215,255,0.52),_transparent_72%)] blur-3xl"
            data-float="medium"
          />

          <div className="relative z-10">
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div>
                <span className="section-badge" data-animate="chip">
                  Step 2 of 3
                </span>
                <h1 className="display-title mt-6 max-w-[12ch] text-[2.9rem] text-white sm:text-[3.8rem]">
                  Connect the datasource.
                </h1>
                <p className="display-copy mt-4 max-w-2xl text-sm sm:text-base">
                  Keep this step focused: save the connection details, then move
                  straight into the workspace.
                </p>
              </div>

              <div className="flex flex-wrap gap-2">
                {['Access ready', 'Datasource setup', 'Workspace next'].map(
                  (item, index) => (
                    <span
                      key={item}
                      className={`small-chip ${index === 1 ? 'border-cyan-300/20 bg-cyan-300/10 text-cyan-100' : ''}`}
                      data-animate="chip"
                    >
                      {item}
                    </span>
                  )
                )}
              </div>
            </div>

            <div className="mt-8 grid grid-cols-1 gap-4 md:grid-cols-2">
              <div className="space-y-2">
                <label className="text-sm font-medium text-zinc-300">
                  Connection name
                </label>
                <input
                  value={form.name}
                  onChange={(event) => updateField('name', event.target.value)}
                  placeholder="My app database"
                  className="input-shell"
                />
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium text-zinc-300">
                  Database type
                </label>
                <AnimatedSelect
                  value={form.dbType}
                  onChange={(value) => updateDbType(value as DatabaseOption)}
                  options={DATABASE_OPTIONS.map((option) => ({
                    value: option.value,
                    label: option.label,
                    description: `Default port ${option.defaultPort}`,
                  }))}
                  ariaLabel="Database type"
                />
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium text-zinc-300">
                  Host (IP or domain)
                </label>
                <input
                  value={form.host}
                  onChange={(event) => updateField('host', event.target.value)}
                  placeholder="db.example.com or 192.168.1.10"
                  className="input-shell"
                />
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium text-zinc-300">Port</label>
                <input
                  value={form.port}
                  onChange={(event) => updateField('port', event.target.value)}
                  placeholder="5432"
                  className="input-shell"
                />
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium text-zinc-300">
                  Database name
                </label>
                <input
                  value={form.databaseName}
                  onChange={(event) =>
                    updateField('databaseName', event.target.value)
                  }
                  placeholder="my_database"
                  className="input-shell"
                />
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium text-zinc-300">
                  Schema
                </label>
                <input
                  value={form.schemaName}
                  onChange={(event) => updateField('schemaName', event.target.value)}
                  placeholder="public"
                  className="input-shell"
                />
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium text-zinc-300">Username</label>
                <input
                  value={form.username}
                  onChange={(event) => updateField('username', event.target.value)}
                  placeholder="readonly_user"
                  className="input-shell"
                />
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium text-zinc-300">Password</label>
                <input
                  type="password"
                  value={form.encryptedPassword}
                  onChange={(event) =>
                    updateField('encryptedPassword', event.target.value)
                  }
                  placeholder="Enter password"
                  className="input-shell"
                />
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium text-zinc-300">
                  Is SSL required?
                </label>
                <AnimatedSelect
                  value={form.sslEnabled ? 'yes' : 'no'}
                  onChange={(value) => updateField('sslEnabled', value === 'yes')}
                  options={SSL_REQUIRED_OPTIONS}
                  ariaLabel="SSL required"
                />
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium text-zinc-300">SSL type</label>
                <AnimatedSelect
                  value={form.sslMode}
                  onChange={(value) => updateField('sslMode', value as SslOption)}
                  options={SSL_OPTIONS.map((option) => ({
                    value: option.value,
                    label: option.label,
                  }))}
                  disabled={!form.sslEnabled}
                  ariaLabel="SSL type"
                />
              </div>
            </div>

            {error && (
              <div className="mt-6 rounded-[24px] border border-red-500/20 bg-red-500/8 p-4 text-sm text-red-200">
                <p className="font-medium">{error.message}</p>
                {error.status && (
                  <p className="mt-2 text-xs text-red-100/80">Status: {error.status}</p>
                )}
                {error.path && (
                  <p className="text-xs text-red-100/80">Path: {error.path}</p>
                )}
                {error.timestamp && (
                  <p className="text-xs text-red-100/80">
                    Time: {new Date(error.timestamp).toLocaleString()}
                  </p>
                )}
              </div>
            )}

            {success && (
              <div className="mt-6 rounded-[24px] border border-emerald-400/20 bg-emerald-400/8 p-4 text-sm text-emerald-200">
                {success}
              </div>
            )}

            <div className="subtle-divider mt-6" data-animate="line" />

            <div className="mt-6 flex flex-wrap items-center justify-between gap-4">
              <span className="rounded-full border border-white/8 bg-white/[0.03] px-4 py-2 text-sm text-zinc-300">
                Stored locally first, then synced to the backend flow
              </span>

              <button
                type="button"
                onClick={handleSave}
                disabled={loading}
                className="primary-button"
                data-pressable
                data-glow={
                  form.host && form.databaseName && form.username && form.encryptedPassword
                    ? 'pulse'
                    : undefined
                }
              >
                {loading ? 'Saving...' : 'Continue to workspace'}
              </button>
            </div>
          </div>
        </section>

        <aside className="grid gap-6">
          <section className="surface-panel px-6 py-7" data-animate="panel">
            <div className="relative z-10">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">
                    Summary
                  </p>
                  <h2 className="mt-3 text-2xl font-semibold text-white">
                    {payloadPreview.name}
                  </h2>
                </div>
                <span className="small-chip">
                  {form.dbType === 'POSTGRESQL' ? 'PostgreSQL' : 'MySQL'}
                </span>
              </div>

              <div className="mt-5 grid gap-3">
                {[
                  {
                    label: 'Host',
                    value: form.host || 'Awaiting input',
                  },
                  {
                    label: 'Database',
                    value: form.databaseName || 'Awaiting input',
                  },
                  {
                    label: 'User',
                    value: form.username || 'Awaiting input',
                  },
                  {
                    label: 'SSL',
                    value: form.sslEnabled ? form.sslMode : 'Not required',
                  },
                ].map((item) => (
                  <div
                    key={item.label}
                    className="rounded-[20px] border border-white/8 bg-white/[0.03] p-4"
                  >
                    <p className="text-xs uppercase tracking-[0.2em] text-zinc-500">
                      {item.label}
                    </p>
                    <p className="mt-2 truncate text-sm font-medium text-white">
                      {item.value}
                    </p>
                  </div>
                ))}
              </div>
            </div>
          </section>

          <section className="surface-panel px-6 py-7" data-animate="panel">
            <div className="relative z-10">
              <div className="flex items-center justify-between gap-3">
                <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">
                  Payload preview
                </p>
                <span className="small-chip">Backend compatible</span>
              </div>
              <pre className="data-block mt-4">
                {JSON.stringify(payloadPreview, null, 2)}
              </pre>
            </div>
          </section>
        </aside>
      </div>
    </main>
  )
}
