import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { saveDataSource } from '../api/dataSourceApi'
import { storeSavedDatasourceDetails } from '../lib/appState'
import { ApiRequestError } from '../api/http'
import { useElegantAnimations } from '../hooks/useElegantAnimations'

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

const onboardingNotes = [
  'Capture the datasource cleanly once, then move straight into the playground.',
  'Keep the JSON payload visible so it is obvious what the client will persist.',
  'Preserve SSL intent and schema details without forcing a cluttered form.',
]

const polishCards = [
  {
    title: 'Secure intake',
    copy: 'Credentials stay aligned with the existing storage flow while the UI surfaces clearer status and calmer emphasis.',
  },
  {
    title: 'Preview-first',
    copy: 'A live payload preview gives you confidence before moving into the query workspace.',
  },
]

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
    <main ref={rootRef} className="page-shell py-6 sm:py-8" data-animate="scene">
      <div className="grid gap-6 xl:grid-cols-[1.12fr_0.88fr]">
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
            <span className="section-badge" data-animate="chip">
              Step 01 / Datasource Intake
            </span>

            <h1 className="display-title mt-7 max-w-3xl text-[3rem] text-white sm:text-[4rem] lg:text-[4.7rem]">
              Connect the right database with less friction.
            </h1>

            <p className="display-copy mt-5 max-w-2xl text-sm sm:text-base">
              This step keeps the existing behavior intact, but reframes it with a more elegant flow:
              calmer visuals, clearer grouping, and subtle motion that helps the form feel responsive
              instead of mechanical.
            </p>

            <div className="mt-7 flex flex-wrap gap-3">
              {onboardingNotes.map((note) => (
                <span key={note} className="small-chip" data-animate="chip">
                  <span className="h-1.5 w-1.5 rounded-full bg-cyan-300" />
                  {note}
                </span>
              ))}
            </div>

            <div className="mt-8 grid gap-4 md:grid-cols-2">
              {polishCards.map((card) => (
                <article
                  key={card.title}
                  className="surface-card p-5"
                  data-animate="panel"
                  data-pressable
                  data-tilt
                >
                  <div className="mb-4 inline-flex rounded-full border border-white/8 bg-white/5 px-3 py-1 text-[11px] uppercase tracking-[0.24em] text-cyan-200">
                    Flow
                  </div>
                  <h2 className="text-lg font-semibold text-white">{card.title}</h2>
                  <p className="mt-3 text-sm leading-6 text-zinc-400">{card.copy}</p>
                </article>
              ))}
            </div>
          </div>
        </section>

        <section className="surface-panel px-6 py-7 sm:px-8 sm:py-8" data-animate="panel">
          <div className="relative z-10 grid gap-4">
            <div className="surface-card p-5" data-tilt>
              <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">
                Draft summary
              </p>
              <div className="mt-4 grid grid-cols-2 gap-3">
                <div className="rounded-[22px] border border-white/8 bg-white/[0.03] p-4">
                  <p className="text-xs text-zinc-500">Database type</p>
                  <p className="mt-2 text-lg font-semibold text-white">
                    {form.dbType === 'POSTGRESQL' ? 'PostgreSQL' : 'MySQL'}
                  </p>
                </div>
                <div className="rounded-[22px] border border-white/8 bg-white/[0.03] p-4">
                  <p className="text-xs text-zinc-500">SSL</p>
                  <p className="mt-2 text-lg font-semibold text-white">
                    {form.sslEnabled ? form.sslMode : 'Not required'}
                  </p>
                </div>
                <div className="rounded-[22px] border border-white/8 bg-white/[0.03] p-4">
                  <p className="text-xs text-zinc-500">Host</p>
                  <p className="mt-2 truncate text-lg font-semibold text-white">
                    {form.host || 'Awaiting input'}
                  </p>
                </div>
                <div className="rounded-[22px] border border-white/8 bg-white/[0.03] p-4">
                  <p className="text-xs text-zinc-500">Database</p>
                  <p className="mt-2 truncate text-lg font-semibold text-white">
                    {form.databaseName || 'Awaiting input'}
                  </p>
                </div>
              </div>
            </div>

            <div className="surface-card p-5" data-tilt>
              <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">
                Payload preview
              </p>
              <pre className="data-block mt-4">{JSON.stringify(payloadPreview, null, 2)}</pre>
            </div>
          </div>
        </section>
      </div>

      <div className="mt-6 grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
        <section className="surface-panel px-6 py-7 sm:px-8 sm:py-8" data-animate="panel">
          <div className="relative z-10">
            <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
              <div>
                <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">
                  Connection form
                </p>
                <h2 className="display-title mt-3 text-[2.4rem] text-white">
                  Configure the source
                </h2>
              </div>

              <div className="rounded-full border border-cyan-400/20 bg-cyan-400/8 px-4 py-2 text-sm text-cyan-100">
                Demo-friendly and backend-compatible
              </div>
            </div>

            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
              <div className="space-y-2">
                <label className="text-sm font-medium text-zinc-300">
                  Connection name
                </label>
                <input
                  value={form.name}
                  onChange={(e) => updateField('name', e.target.value)}
                  placeholder="My app database"
                  className="input-shell"
                />
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium text-zinc-300">
                  Database type
                </label>
                <select
                  value={form.dbType}
                  onChange={(e) => updateDbType(e.target.value as DatabaseOption)}
                  className="input-shell"
                >
                  {DATABASE_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium text-zinc-300">
                  Host (IP or domain)
                </label>
                <input
                  value={form.host}
                  onChange={(e) => updateField('host', e.target.value)}
                  placeholder="db.example.com or 192.168.1.10"
                  className="input-shell"
                />
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium text-zinc-300">Port</label>
                <input
                  value={form.port}
                  onChange={(e) => updateField('port', e.target.value)}
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
                  onChange={(e) => updateField('databaseName', e.target.value)}
                  placeholder="my_database"
                  className="input-shell"
                />
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium text-zinc-300">
                  Schema (optional)
                </label>
                <input
                  value={form.schemaName}
                  onChange={(e) => updateField('schemaName', e.target.value)}
                  placeholder="public"
                  className="input-shell"
                />
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium text-zinc-300">Username</label>
                <input
                  value={form.username}
                  onChange={(e) => updateField('username', e.target.value)}
                  placeholder="readonly_user"
                  className="input-shell"
                />
              </div>

              <div className="space-y-2 md:col-span-2">
                <label className="text-sm font-medium text-zinc-300">Password</label>
                <input
                  type="password"
                  value={form.encryptedPassword}
                  onChange={(e) => updateField('encryptedPassword', e.target.value)}
                  placeholder="Enter password"
                  className="input-shell"
                />
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium text-zinc-300">
                  Is SSL required?
                </label>
                <select
                  value={form.sslEnabled ? 'yes' : 'no'}
                  onChange={(e) => updateSslEnabled(e.target.value === 'yes')}
                  className="input-shell"
                >
                  <option value="no">No</option>
                  <option value="yes">Yes</option>
                </select>
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium text-zinc-300">SSL type</label>
                <select
                  value={form.sslMode}
                  onChange={(e) => updateField('sslMode', e.target.value as SslOption)}
                  className="input-shell disabled:opacity-50"
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
                <p className="mt-3 text-xs text-red-100/80">
                  Check the host, port, database name, username, and SSL settings if this keeps happening.
                </p>
              </div>
            )}

            {success && (
              <div className="mt-6 rounded-[24px] border border-emerald-400/20 bg-emerald-400/8 p-4 text-sm text-emerald-200">
                {success}
              </div>
            )}

            <div className="subtle-divider mt-6" data-animate="line" />

            <div className="mt-6 flex flex-wrap items-center justify-between gap-4">
              <p className="max-w-xl text-sm leading-6 text-zinc-400">
                When you save, SigmaQL keeps the local summary for the workspace and then moves you into the playground flow.
              </p>

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
                {loading ? 'Saving...' : 'Save details'}
              </button>
            </div>
          </div>
        </section>

        <aside className="grid gap-6">
          <section className="surface-panel px-6 py-7" data-animate="panel">
            <div className="relative z-10">
              <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">
                What this screen improves
              </p>
              <div className="mt-5 space-y-4">
                {[
                  'Field grouping now reads like a guided intake instead of a generic admin form.',
                  'The payload preview mirrors what gets stored so the state feels transparent.',
                  'Animations reinforce interaction without overwhelming the darker, editorial aesthetic.',
                ].map((item) => (
                  <div key={item} className="surface-card p-4" data-pressable data-tilt>
                    <p className="text-sm leading-6 text-zinc-300">{item}</p>
                  </div>
                ))}
              </div>
            </div>
          </section>

          <section className="surface-panel px-6 py-7" data-animate="panel">
            <div className="relative z-10">
              <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">
                Completion state
              </p>
              <div className="mt-5 grid grid-cols-2 gap-3">
                <div className="rounded-[22px] border border-white/8 bg-white/[0.03] p-4" data-tilt>
                  <p className="text-xs text-zinc-500">Host ready</p>
                  <p className="mt-2 text-lg font-semibold text-white">
                    {form.host ? 'Yes' : 'No'}
                  </p>
                </div>
                <div className="rounded-[22px] border border-white/8 bg-white/[0.03] p-4" data-tilt>
                  <p className="text-xs text-zinc-500">Auth ready</p>
                  <p className="mt-2 text-lg font-semibold text-white">
                    {form.username && form.encryptedPassword ? 'Yes' : 'No'}
                  </p>
                </div>
              </div>
            </div>
          </section>
        </aside>
      </div>
    </main>
  )
}
