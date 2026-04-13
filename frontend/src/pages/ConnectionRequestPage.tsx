import { startTransition, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { logOutUser } from '../api/accountApi'
import {
  createDataSource,
  deleteDataSource,
  listDataSources,
  testDataSourceConnection,
  updateDataSource,
} from '../api/dataSourceApi'
import { ApiRequestError } from '../api/http'
import { generateSchema, refreshSchema } from '../api/runtimeApi'
import { AnimatedSelect } from '../components/AnimatedSelect'
import { useElegantAnimations } from '../hooks/useElegantAnimations'
import { clearSavedDatasource, clearSessionActive, getStoredDatasourceDetails, storeSelectedDatasource } from '../lib/appState'
import { formatDateTime, getStatusLabel, getStatusTone } from '../lib/platform'
import type { DatasourcePayload, DatasourceRecord, SslMode } from '../types/platform'

const SSL_OPTIONS = [
  { value: 'DISABLE', label: 'Disable' },
  { value: 'PREFER', label: 'Prefer' },
  { value: 'REQUIRE', label: 'Require' },
  { value: 'VERIFY_CA', label: 'Verify CA' },
  { value: 'VERIFY_FULL', label: 'Verify full' },
] as const

const DATABASE_OPTIONS = [
  {
    value: 'POSTGRESQL',
    label: 'PostgreSQL',
    description: 'The current runtime engine supports PostgreSQL datasources.',
  },
] as const

type FormState = {
  displayName: string
  host: string
  port: string
  databaseName: string
  schemaName: string
  username: string
  password: string
  sslMode: SslMode
  connectTimeoutMs: string
  socketTimeoutMs: string
  applicationName: string
  extraJdbcOptionsJson: string
}

type FeedbackTone = 'success' | 'danger' | 'warning' | 'neutral'
type Feedback = { tone: FeedbackTone; title: string; message: string }

const EMPTY_FORM: FormState = {
  displayName: '',
  host: '',
  port: '5432',
  databaseName: '',
  schemaName: 'public',
  username: '',
  password: '',
  sslMode: 'PREFER',
  connectTimeoutMs: '',
  socketTimeoutMs: '',
  applicationName: '',
  extraJdbcOptionsJson: '',
}

function toForm(datasource: DatasourceRecord): FormState {
  return {
    displayName: datasource.displayName,
    host: datasource.host,
    port: String(datasource.port),
    databaseName: datasource.databaseName,
    schemaName: datasource.schemaName,
    username: datasource.username,
    password: '',
    sslMode: datasource.sslMode,
    connectTimeoutMs: datasource.connectTimeoutMs == null ? '' : String(datasource.connectTimeoutMs),
    socketTimeoutMs: datasource.socketTimeoutMs == null ? '' : String(datasource.socketTimeoutMs),
    applicationName: datasource.applicationName ?? '',
    extraJdbcOptionsJson: datasource.extraJdbcOptionsJson ?? '',
  }
}

function toPayload(form: FormState, mode: 'create' | 'edit'): DatasourcePayload {
  const trimmedJson = form.extraJdbcOptionsJson.trim()
  if (!form.displayName.trim()) throw new Error('Datasource name is required.')
  if (!form.host.trim()) throw new Error('Host is required.')
  if (!form.port.trim() || Number.isNaN(Number(form.port))) throw new Error('Port must be numeric.')
  if (Number(form.port) < 1 || Number(form.port) > 65535) throw new Error('Port must be between 1 and 65535.')
  if (!form.databaseName.trim()) throw new Error('Database name is required.')
  if (!form.schemaName.trim()) throw new Error('Schema name is required.')
  if (!form.username.trim()) throw new Error('Username is required.')
  if (mode === 'create' && !form.password) throw new Error('Password is required for a new datasource.')
  if (form.connectTimeoutMs.trim() && (!Number.isFinite(Number(form.connectTimeoutMs)) || Number(form.connectTimeoutMs) < 1)) {
    throw new Error('Connect timeout must be a positive number.')
  }
  if (form.socketTimeoutMs.trim() && (!Number.isFinite(Number(form.socketTimeoutMs)) || Number(form.socketTimeoutMs) < 1)) {
    throw new Error('Socket timeout must be a positive number.')
  }
  if (trimmedJson) {
    try {
      JSON.parse(trimmedJson)
    } catch {
      throw new Error('Extra connection options must be valid JSON.')
    }
  }
  return {
    displayName: form.displayName.trim(),
    dbType: 'POSTGRESQL',
    host: form.host.trim(),
    port: Number(form.port),
    databaseName: form.databaseName.trim(),
    schemaName: form.schemaName.trim(),
    username: form.username.trim(),
    password: form.password,
    sslMode: form.sslMode,
    connectTimeoutMs: form.connectTimeoutMs.trim() ? Number(form.connectTimeoutMs) : null,
    socketTimeoutMs: form.socketTimeoutMs.trim() ? Number(form.socketTimeoutMs) : null,
    applicationName: form.applicationName.trim() || null,
    sslRootCertRef: null,
    extraJdbcOptionsJson: trimmedJson || null,
  }
}

function feedbackClass(tone: FeedbackTone) {
  switch (tone) {
    case 'success':
      return 'border-emerald-400/20 bg-emerald-400/8 text-emerald-100'
    case 'danger':
      return 'border-red-500/20 bg-red-500/8 text-red-100'
    case 'warning':
      return 'border-amber-400/20 bg-amber-400/8 text-amber-100'
    default:
      return 'border-white/10 bg-white/[0.04] text-zinc-100'
  }
}

function StatusPill({ status }: { status: DatasourceRecord['lastConnectionStatus'] | null }) {
  return <span className={`status-pill status-pill-${getStatusTone(status)}`}>{getStatusLabel(status)}</span>
}

export default function ConnectionRequestPage() {
  const navigate = useNavigate()
  const rootRef = useElegantAnimations<HTMLDivElement>([])
  const storedSelection = getStoredDatasourceDetails()
  const [datasources, setDatasources] = useState<DatasourceRecord[]>([])
  const [selectedId, setSelectedId] = useState<number | null>(storedSelection?.id ?? null)
  const [form, setForm] = useState<FormState>(EMPTY_FORM)
  const [mode, setMode] = useState<'create' | 'edit'>('create')
  const [editingId, setEditingId] = useState<number | null>(null)
  const [feedback, setFeedback] = useState<Feedback | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [testingId, setTestingId] = useState<number | null>(null)
  const [generatingId, setGeneratingId] = useState<number | null>(null)
  const [refreshingId, setRefreshingId] = useState<number | null>(null)
  const [deletingId, setDeletingId] = useState<number | null>(null)
  const [pendingDelete, setPendingDelete] = useState<DatasourceRecord | null>(null)

  const selectedDatasource = useMemo(
    () => datasources.find((item) => item.id === selectedId) ?? null,
    [datasources, selectedId]
  )

  useEffect(() => {
    void reload()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function reload(preferredId?: number | null) {
    try {
      setLoading(true)
      const next = await listDataSources()
      setDatasources(next)
      const candidateId = preferredId ?? selectedId ?? storedSelection?.id ?? next[0]?.id ?? null
      const candidate = next.find((item) => item.id === candidateId) ?? next[0] ?? null
      startTransition(() => setSelectedId(candidate?.id ?? null))
      if (!candidate) {
        clearSavedDatasource()
      }
    } catch (error) {
      setFeedback({
        tone: 'danger',
        title: 'Datasource catalog unavailable',
        message: error instanceof Error ? error.message : 'Could not load datasources.',
      })
    } finally {
      setLoading(false)
    }
  }

  function updateField<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((current) => ({ ...current, [key]: value }))
  }

  function resetCreateMode() {
    setMode('create')
    setEditingId(null)
    setForm(EMPTY_FORM)
    setFeedback(null)
  }

  function startEdit(datasource: DatasourceRecord) {
    setMode('edit')
    setEditingId(datasource.id)
    setForm(toForm(datasource))
    setFeedback({
      tone: 'neutral',
      title: 'Editing datasource',
      message: `Updating ${datasource.displayName}. Leave the password blank to keep the current secret.`,
    })
  }

  async function saveDatasource() {
    try {
      const payload = toPayload(form, mode)
      setSaving(true)
      if (mode === 'edit' && editingId != null) {
        await updateDataSource(editingId, payload)
      } else {
        await createDataSource(payload)
      }
      await reload(editingId)
      if (mode === 'create') setForm(EMPTY_FORM)
      setFeedback({
        tone: 'success',
        title: mode === 'edit' ? 'Datasource updated' : 'Datasource created',
        message: 'Run a connection test, then generate the schema snapshot to open the explorer.',
      })
    } catch (error) {
      if (
        error instanceof ApiRequestError &&
        (error.status === 401 || error.status === 403)
      ) {
        const localSelectionId = editingId ?? Date.now()
        storeSelectedDatasource({
          id: localSelectionId,
          displayName: form.displayName.trim() || 'Local preview datasource',
          dbType: 'POSTGRESQL',
          host: form.host.trim(),
          port: Number(form.port),
          databaseName: form.databaseName.trim(),
          schemaName: form.schemaName.trim(),
          username: form.username.trim(),
          lastSchemaGeneratedAt: new Date().toISOString(),
          lastSchemaFingerprint: 'local-preview',
        })
        setFeedback({
          tone: 'warning',
          title: 'Backend session unavailable',
          message: 'The datasource could not be saved to the backend, so the app will open a local preview workspace instead.',
        })
        setTimeout(() => {
          navigate('/playground', { replace: true })
        }, 350)
        return
      }

      setFeedback({
        tone: 'danger',
        title: 'Datasource could not be saved',
        message: error instanceof Error ? error.message : 'Saving datasource failed.',
      })
    } finally {
      setSaving(false)
    }
  }

  async function verifyDatasource(datasource: DatasourceRecord) {
    try {
      setTestingId(datasource.id)
      const result = await testDataSourceConnection(datasource.id)
      await reload(datasource.id)
      setFeedback({
        tone: result.successful ? 'success' : 'warning',
        title: result.successful ? 'Connection verified' : 'Connection needs attention',
        message: result.message,
      })
    } catch (error) {
      if (
        error instanceof ApiRequestError &&
        (error.status === 401 || error.status === 403)
      ) {
        clearSavedDatasource()
        clearSessionActive()
        navigate('/login', { replace: true })
        return
      }

      setFeedback({
        tone: 'danger',
        title: 'Connection test failed',
        message: error instanceof Error ? error.message : 'Could not verify the datasource.',
      })
    } finally {
      setTestingId(null)
    }
  }

  async function syncSchema(datasource: DatasourceRecord, mode: 'generate' | 'refresh') {
    try {
      mode === 'generate' ? setGeneratingId(datasource.id) : setRefreshingId(datasource.id)
      const schema =
        mode === 'generate'
          ? await generateSchema(datasource.id)
          : await refreshSchema(datasource.id)
      await reload(datasource.id)
      setFeedback({
        tone: 'success',
        title: mode === 'generate' ? 'Schema generated' : 'Schema refreshed',
        message: `${Object.keys(schema.tables).length} schema surfaces are ready to explore.`,
      })
    } catch (error) {
      if (
        error instanceof ApiRequestError &&
        (error.status === 401 || error.status === 403)
      ) {
        clearSavedDatasource()
        clearSessionActive()
        navigate('/login', { replace: true })
        return
      }

      setFeedback({
        tone: 'danger',
        title: mode === 'generate' ? 'Schema generation failed' : 'Schema refresh failed',
        message: error instanceof Error ? error.message : 'Schema action failed.',
      })
    } finally {
      setGeneratingId(null)
      setRefreshingId(null)
    }
  }

  async function removeDatasource() {
    if (!pendingDelete) return
    try {
      setDeletingId(pendingDelete.id)
      await deleteDataSource(pendingDelete.id)
      if (selectedId === pendingDelete.id) clearSavedDatasource()
      await reload(selectedId === pendingDelete.id ? null : selectedId)
      setFeedback({
        tone: 'success',
        title: 'Datasource removed',
        message: `${pendingDelete.displayName} was removed from the workspace.`,
      })
    } catch (error) {
      if (
        error instanceof ApiRequestError &&
        (error.status === 401 || error.status === 403)
      ) {
        clearSavedDatasource()
        clearSessionActive()
        navigate('/login', { replace: true })
        return
      }

      setFeedback({
        tone: 'danger',
        title: 'Datasource could not be deleted',
        message: error instanceof Error ? error.message : 'Delete request failed.',
      })
    } finally {
      setDeletingId(null)
      setPendingDelete(null)
    }
  }

  async function handleLogOut() {
    try {
      await logOutUser()
    } catch {
      // Keep the local cleanup path even if the backend session already expired.
    } finally {
      clearSavedDatasource()
      clearSessionActive()
      navigate('/login', { replace: true })
    }
  }

  function openExplorer(datasource: DatasourceRecord) {
    storeSelectedDatasource({
      id: datasource.id,
      displayName: datasource.displayName,
      dbType: datasource.dbType,
      host: datasource.host,
      port: datasource.port,
      databaseName: datasource.databaseName,
      schemaName: datasource.schemaName,
      username: datasource.username,
      lastSchemaGeneratedAt: datasource.lastSchemaGeneratedAt,
      lastSchemaFingerprint: datasource.lastSchemaFingerprint,
    })
    navigate('/playground')
  }

  const schemaUnlocked = selectedDatasource?.lastConnectionStatus === 'SUCCEEDED'
  const explorerUnlocked = Boolean(selectedDatasource?.lastSchemaGeneratedAt)

  return (
    <main ref={rootRef} className="page-shell py-6 sm:py-8" data-animate="scene">
      <div className="workspace-toolbar">
        <div className="flex flex-wrap items-center justify-end gap-3 pointer-events-auto">
          <button
            type="button"
            className="workspace-menu-trigger"
            onClick={handleLogOut}
            data-pressable
          >
            <span>
              <span className="workspace-menu-label">Session</span>
              <span className="workspace-menu-value">Log out</span>
            </span>
            <span className="workspace-menu-caret" />
          </button>
        </div>
      </div>

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1.08fr)_400px]">
        <section className="surface-panel surface-overflow-visible px-6 py-7 sm:px-8 sm:py-8 lg:px-10" data-animate="hero" data-reveal="right">
          <div className="relative z-10">
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div>
                <span className="section-badge" data-animate="chip">Datasource management</span>
                <h1 className="display-title mt-6 max-w-[11ch] text-[3rem] text-white sm:text-[3.9rem]">Connect, verify, and unlock schema.</h1>
                <p className="display-copy mt-4 max-w-2xl text-sm sm:text-base">Create the datasource, validate connectivity, and generate the runtime snapshot before opening the explorer.</p>
              </div>
              <div className="flex flex-wrap gap-2">
                {['Connection lifecycle', 'Schema generation', 'Explorer handoff'].map((item) => <span key={item} className="small-chip" data-animate="chip">{item}</span>)}
              </div>
            </div>

            {feedback && (
              <div className={`mt-6 rounded-[24px] border p-4 ${feedbackClass(feedback.tone)}`} data-reveal="up">
                <p className="text-sm font-semibold">{feedback.title}</p>
                <p className="mt-2 text-sm opacity-90">{feedback.message}</p>
              </div>
            )}

            <div className="mt-8 grid gap-4 md:grid-cols-2">
              <div className="space-y-2">
                <label className="text-sm font-medium text-zinc-300">Datasource name</label>
                <input value={form.displayName} onChange={(event) => updateField('displayName', event.target.value)} placeholder="Production analytics" className="input-shell" />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium text-zinc-300">Database engine</label>
                <AnimatedSelect value="POSTGRESQL" onChange={() => undefined} options={DATABASE_OPTIONS} ariaLabel="Database engine" disabled />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium text-zinc-300">Host</label>
                <input value={form.host} onChange={(event) => updateField('host', event.target.value)} placeholder="db.internal.example" className="input-shell" />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium text-zinc-300">Port</label>
                <input value={form.port} onChange={(event) => updateField('port', event.target.value)} placeholder="5432" className="input-shell" />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium text-zinc-300">Database name</label>
                <input value={form.databaseName} onChange={(event) => updateField('databaseName', event.target.value)} placeholder="warehouse" className="input-shell" />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium text-zinc-300">Schema name</label>
                <input value={form.schemaName} onChange={(event) => updateField('schemaName', event.target.value)} placeholder="public" className="input-shell" />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium text-zinc-300">Username</label>
                <input value={form.username} onChange={(event) => updateField('username', event.target.value)} placeholder="forgeql_app" className="input-shell" />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium text-zinc-300">Password {mode === 'edit' ? '(leave blank to keep existing)' : ''}</label>
                <input value={form.password} onChange={(event) => updateField('password', event.target.value)} type="password" placeholder={mode === 'edit' ? 'Leave blank to keep current secret' : 'Enter password'} className="input-shell" />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium text-zinc-300">SSL mode</label>
                <AnimatedSelect value={form.sslMode} onChange={(value) => updateField('sslMode', value as SslMode)} options={SSL_OPTIONS} ariaLabel="SSL mode" />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium text-zinc-300">Application name</label>
                <input value={form.applicationName} onChange={(event) => updateField('applicationName', event.target.value)} placeholder="ForgeQL Console" className="input-shell" />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium text-zinc-300">Connect timeout (ms)</label>
                <input value={form.connectTimeoutMs} onChange={(event) => updateField('connectTimeoutMs', event.target.value)} placeholder="5000" className="input-shell" />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium text-zinc-300">Socket timeout (ms)</label>
                <input value={form.socketTimeoutMs} onChange={(event) => updateField('socketTimeoutMs', event.target.value)} placeholder="30000" className="input-shell" />
              </div>
              <div className="space-y-2 md:col-span-2">
                <label className="text-sm font-medium text-zinc-300">Extra connection options (JSON)</label>
                <textarea value={form.extraJdbcOptionsJson} onChange={(event) => updateField('extraJdbcOptionsJson', event.target.value)} placeholder='{"reWriteBatchedInserts": true}' className="input-shell min-h-[124px] resize-y" />
              </div>
            </div>

            <div className="subtle-divider mt-6" data-animate="line" />
            <div className="mt-6 flex flex-wrap items-center justify-between gap-4">
              <div className="flex flex-wrap gap-3">
                <button type="button" className="secondary-button" onClick={resetCreateMode} data-pressable>New datasource</button>
                <button type="button" className="primary-button" onClick={saveDatasource} disabled={saving} data-pressable>
                  {saving ? 'Saving...' : mode === 'edit' ? 'Update datasource' : 'Create datasource'}
                </button>
              </div>
              <span className="rounded-full border border-white/8 bg-white/[0.03] px-4 py-2 text-sm text-zinc-300">
                Schema actions stay locked until the connection succeeds
              </span>
            </div>
          </div>
        </section>

        <aside className="grid gap-6">
          <section className="surface-panel px-6 py-7" data-animate="panel" data-reveal="left">
            <div className="relative z-10">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">Selected datasource</p>
                  <h2 className="mt-3 text-2xl font-semibold text-white">{selectedDatasource?.displayName ?? 'No datasource selected'}</h2>
                </div>
                <StatusPill status={selectedDatasource?.lastConnectionStatus ?? null} />
              </div>

              {selectedDatasource ? (
                <>
                  <div className="mt-5 grid gap-3">
                    {[
                      { label: 'Endpoint', value: `${selectedDatasource.host}:${selectedDatasource.port}` },
                      { label: 'Database', value: `${selectedDatasource.databaseName} / ${selectedDatasource.schemaName}` },
                      { label: 'Last test', value: formatDateTime(selectedDatasource.lastConnectionTestAt) },
                      { label: 'Last schema sync', value: formatDateTime(selectedDatasource.lastSchemaGeneratedAt) },
                    ].map((item) => (
                      <div key={item.label} className="rounded-[20px] border border-white/8 bg-white/[0.03] p-4">
                        <p className="text-xs uppercase tracking-[0.2em] text-zinc-500">{item.label}</p>
                        <p className="mt-2 text-sm font-medium text-white">{item.value}</p>
                      </div>
                    ))}
                  </div>

                  {selectedDatasource.lastConnectionError && (
                    <div className="mt-4 rounded-[22px] border border-red-500/20 bg-red-500/8 p-4 text-sm text-red-100">
                      {selectedDatasource.lastConnectionError}
                    </div>
                  )}

                  <div className="mt-5 grid gap-3 sm:grid-cols-2">
                    <button type="button" className="secondary-button" onClick={() => verifyDatasource(selectedDatasource)} disabled={testingId === selectedDatasource.id} data-pressable>
                      {testingId === selectedDatasource.id ? 'Testing...' : 'Test connection'}
                    </button>
                    <button type="button" className="secondary-button" onClick={() => syncSchema(selectedDatasource, 'generate')} disabled={!schemaUnlocked || generatingId === selectedDatasource.id} data-pressable>
                      {generatingId === selectedDatasource.id ? 'Generating...' : 'Generate schema'}
                    </button>
                    <button type="button" className="secondary-button" onClick={() => syncSchema(selectedDatasource, 'refresh')} disabled={!schemaUnlocked || !selectedDatasource.lastSchemaGeneratedAt || refreshingId === selectedDatasource.id} data-pressable>
                      {refreshingId === selectedDatasource.id ? 'Refreshing...' : 'Refresh schema'}
                    </button>
                    <button type="button" className="primary-button" onClick={() => openExplorer(selectedDatasource)} disabled={!explorerUnlocked} data-pressable>
                      Open explorer
                    </button>
                  </div>
                </>
              ) : (
                <div className="mt-5 rounded-[22px] border border-white/8 bg-white/[0.03] p-4 text-sm text-zinc-400">
                  Create or select a datasource to unlock testing and schema actions.
                </div>
              )}
            </div>
          </section>

          <section className="surface-panel px-6 py-7" data-animate="panel" data-reveal="left">
            <div className="relative z-10">
              <div className="flex items-end justify-between gap-3">
                <div>
                  <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">Datasource catalog</p>
                  <h2 className="mt-3 text-2xl font-semibold text-white">Registered connections</h2>
                </div>
                <span className="small-chip">{loading ? 'Loading...' : `${datasources.length} total`}</span>
              </div>

              <div className="mt-5 space-y-3">
                {!loading && datasources.length === 0 && (
                  <div className="rounded-[22px] border border-white/8 bg-white/[0.03] p-4 text-sm text-zinc-400">
                    No datasources yet. Create one from the form and it will appear here.
                  </div>
                )}

                {datasources.map((datasource) => {
                  const selected = datasource.id === selectedId
                  const locked = datasource.lastConnectionStatus !== 'SUCCEEDED'
                  return (
                    <article key={datasource.id} className={`surface-card p-4 ${selected ? 'ring-1 ring-cyan-300/30' : ''}`} data-animate="panel" data-reveal="right">
                      <div className="flex items-start justify-between gap-3">
                        <button type="button" className="text-left" onClick={() => setSelectedId(datasource.id)}>
                          <h3 className="text-base font-semibold text-white">{datasource.displayName}</h3>
                          <p className="mt-1 text-sm text-zinc-400">{datasource.databaseName} on {datasource.host}:{datasource.port}</p>
                        </button>
                        <StatusPill status={datasource.lastConnectionStatus ?? null} />
                      </div>

                      <div className="mt-4 flex flex-wrap gap-2">
                        <button type="button" className="secondary-button px-4 py-2" onClick={() => startEdit(datasource)} data-pressable>Edit</button>
                        <button type="button" className="secondary-button px-4 py-2" onClick={() => verifyDatasource(datasource)} disabled={testingId === datasource.id} data-pressable>{testingId === datasource.id ? 'Testing...' : 'Test'}</button>
                        <button type="button" className="secondary-button px-4 py-2" onClick={() => syncSchema(datasource, 'generate')} disabled={locked || generatingId === datasource.id} data-pressable>Generate</button>
                        <button type="button" className="secondary-button px-4 py-2" onClick={() => syncSchema(datasource, 'refresh')} disabled={locked || !datasource.lastSchemaGeneratedAt || refreshingId === datasource.id} data-pressable>Refresh</button>
                        <button type="button" className="secondary-button px-4 py-2 text-red-100" onClick={() => setPendingDelete(datasource)} data-pressable>Delete</button>
                      </div>
                    </article>
                  )
                })}
              </div>
            </div>
          </section>
        </aside>
      </div>

      {pendingDelete && (
        <div className="fixed inset-0 z-[120] flex items-center justify-center bg-slate-950/70 px-4 backdrop-blur-md">
          <div className="surface-panel w-full max-w-md px-6 py-6" data-reveal="up">
            <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">Confirm delete</p>
            <h2 className="mt-3 text-2xl font-semibold text-white">Remove {pendingDelete.displayName}?</h2>
            <p className="mt-4 text-sm leading-6 text-zinc-300">This deletes the datasource record and its cached schema snapshot. The actual PostgreSQL database is not touched.</p>
            <div className="mt-6 flex flex-wrap justify-end gap-3">
              <button type="button" className="secondary-button" onClick={() => setPendingDelete(null)} data-pressable>Cancel</button>
              <button type="button" className="primary-button" onClick={removeDatasource} disabled={deletingId === pendingDelete.id} data-pressable>
                {deletingId === pendingDelete.id ? 'Deleting...' : 'Delete datasource'}
              </button>
            </div>
          </div>
        </div>
      )}
    </main>
  )
}
