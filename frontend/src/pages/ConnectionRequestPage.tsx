import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthProvider'
import {
  createDataSource,
  deleteDataSource,
  updateDataSource,
} from '../api/dataSourceApi'
import { useDatasources } from '../hooks/useDatasources'
import {
  clearSavedDatasource,
  getStoredDatasourceDetails,
  storeSelectedDatasource,
} from '../lib/appState'
import type {
  DatasourcePayload,
  DatasourceRecord,
  GlobalRole,
  SslMode,
} from '../types/platform'
import { DatasourceAccessModal } from '../components/rbac/DatasourceAccessModal'
import { UserManagementPanel } from '../components/rbac/UserManagementPanel'
import { RoleBadge } from '../components/rbac/RoleBadge'
import { Dropdown } from '../components/ui/Dropdown'
import { Button } from '../components/ui/Button'
import { Modal } from '../components/ui/Modal'

type FlashTone = 'success' | 'danger'

type FlashMessage = {
  tone: FlashTone
  message: string
}

type DatasourceEditorState = {
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
  sslRootCertRef: string
  extraJdbcOptionsJson: string
}

const SSL_MODE_OPTIONS: SslMode[] = [
  'DISABLE',
  'PREFER',
  'REQUIRE',
  'VERIFY_CA',
  'VERIFY_FULL',
]

const SSL_MODE_SELECT_OPTIONS = SSL_MODE_OPTIONS.map((option) => ({
  value: option,
  label: option,
}))

function isAdminRole(role?: GlobalRole | null) {
  return role === 'ADMIN' || role === 'MAIN_ADMIN'
}

function isMainAdminRole(role?: GlobalRole | null) {
  return role === 'MAIN_ADMIN'
}

function sslModeRequiresCertificate(sslMode: SslMode) {
  return sslMode === 'VERIFY_CA' || sslMode === 'VERIFY_FULL'
}

function formatConnection(datasource: DatasourceRecord) {
  return `${datasource.host}:${datasource.port} / ${datasource.databaseName}/${datasource.schemaName}`
}

function getCapabilityCopy(datasource: DatasourceRecord, isAdmin: boolean) {
  if (isAdmin) {
    return {
      title: 'Full control',
      description: 'Open the explorer, update the datasource, or manage access from here.',
    }
  }

  if (datasource.accessRole === 'MANAGER') {
    return {
      title: 'Manager access',
      description: 'You can open the explorer and maintain this datasource.',
    }
  }

  return {
    title: 'Read only',
    description: 'This datasource is visible, but management actions stay locked.',
  }
}

function canManageDatasource(globalRole: GlobalRole | undefined) {
  return isMainAdminRole(globalRole)
}

function canManageDatasourceAccess(
  globalRole: GlobalRole | undefined,
  currentUserId: number | undefined,
  datasource: DatasourceRecord
) {
  if (globalRole === 'MAIN_ADMIN') {
    return true
  }

  return globalRole === 'ADMIN' && currentUserId === datasource.ownerUserId
}

function createEmptyEditorState(): DatasourceEditorState {
  return {
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
    sslRootCertRef: '',
    extraJdbcOptionsJson: '',
  }
}

function toStoredSelection(datasource: DatasourceRecord) {
  return {
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
  }
}

function toEditorState(datasource: DatasourceRecord): DatasourceEditorState {
  return {
    displayName: datasource.displayName,
    host: datasource.host,
    port: String(datasource.port),
    databaseName: datasource.databaseName,
    schemaName: datasource.schemaName,
    username: datasource.username,
    password: '',
    sslMode: datasource.sslMode,
    connectTimeoutMs:
      datasource.connectTimeoutMs == null ? '' : String(datasource.connectTimeoutMs),
    socketTimeoutMs:
      datasource.socketTimeoutMs == null ? '' : String(datasource.socketTimeoutMs),
    applicationName: datasource.applicationName ?? '',
    sslRootCertRef: datasource.sslRootCertRef ?? '',
    extraJdbcOptionsJson: datasource.extraJdbcOptionsJson ?? '',
  }
}

function parseOptionalNumber(value: string, label: string) {
  const normalized = value.trim()
  if (!normalized) {
    return null
  }

  const parsed = Number(normalized)
  if (!Number.isInteger(parsed) || parsed < 1) {
    throw new Error(`${label} must be a positive number.`)
  }

  return parsed
}

function buildDatasourcePayload(
  datasource: DatasourceRecord,
  editor: DatasourceEditorState
): DatasourcePayload {
  const port = Number(editor.port.trim())

  if (!Number.isInteger(port) || port < 1 || port > 65535) {
    throw new Error('Port must be between 1 and 65535.')
  }

  if (!editor.displayName.trim()) {
    throw new Error('Display name is required.')
  }

  if (!editor.host.trim()) {
    throw new Error('Host is required.')
  }

  if (!editor.databaseName.trim()) {
    throw new Error('Database name is required.')
  }

  if (!editor.schemaName.trim()) {
    throw new Error('Schema name is required.')
  }

  if (!editor.username.trim()) {
    throw new Error('Username is required.')
  }

  return {
    displayName: editor.displayName.trim(),
    dbType: datasource.dbType,
    host: editor.host.trim(),
    port,
    databaseName: editor.databaseName.trim(),
    schemaName: editor.schemaName.trim(),
    username: editor.username.trim(),
    password: editor.password.trim() ? editor.password.trim() : null,
    sslMode: editor.sslMode,
    connectTimeoutMs: parseOptionalNumber(
      editor.connectTimeoutMs,
      'Connect timeout'
    ),
    socketTimeoutMs: parseOptionalNumber(
      editor.socketTimeoutMs,
      'Socket timeout'
    ),
    applicationName: editor.applicationName.trim() || null,
    sslRootCertRef: sslModeRequiresCertificate(editor.sslMode)
      ? editor.sslRootCertRef.trim() || null
      : null,
    extraJdbcOptionsJson: editor.extraJdbcOptionsJson.trim() || null,
  }
}

function buildNewDatasourcePayload(editor: DatasourceEditorState): DatasourcePayload {
  const port = Number(editor.port.trim())

  if (!Number.isInteger(port) || port < 1 || port > 65535) {
    throw new Error('Port must be between 1 and 65535.')
  }

  if (!editor.displayName.trim()) {
    throw new Error('Display name is required.')
  }

  if (!editor.host.trim()) {
    throw new Error('Host is required.')
  }

  if (!editor.databaseName.trim()) {
    throw new Error('Database name is required.')
  }

  if (!editor.schemaName.trim()) {
    throw new Error('Schema name is required.')
  }

  if (!editor.username.trim()) {
    throw new Error('Username is required.')
  }

  if (!editor.password.trim()) {
    throw new Error('Password is required.')
  }

  return {
    displayName: editor.displayName.trim(),
    dbType: 'POSTGRESQL',
    host: editor.host.trim(),
    port,
    databaseName: editor.databaseName.trim(),
    schemaName: editor.schemaName.trim(),
    username: editor.username.trim(),
    password: editor.password.trim(),
    sslMode: editor.sslMode,
    connectTimeoutMs: parseOptionalNumber(
      editor.connectTimeoutMs,
      'Connect timeout'
    ),
    socketTimeoutMs: parseOptionalNumber(
      editor.socketTimeoutMs,
      'Socket timeout'
    ),
    applicationName: editor.applicationName.trim() || null,
    sslRootCertRef: sslModeRequiresCertificate(editor.sslMode)
      ? editor.sslRootCertRef.trim() || null
      : null,
    extraJdbcOptionsJson: editor.extraJdbcOptionsJson.trim() || null,
  }
}

function flashClassName(tone: FlashTone) {
  return tone === 'success'
    ? 'border-emerald-400/20 bg-emerald-500/10 text-emerald-100'
    : 'border-red-400/20 bg-red-500/10 text-red-100'
}

export default function ConnectionRequestPage() {
  const navigate = useNavigate()
  const { user, logout } = useAuth()
  const [activeDatasource, setActiveDatasource] = useState<DatasourceRecord | null>(null)
  const [creatingDatasource, setCreatingDatasource] = useState(false)
  const [createState, setCreateState] = useState<DatasourceEditorState | null>(null)
  const [createSaving, setCreateSaving] = useState(false)
  const [createError, setCreateError] = useState<string | null>(null)
  const [editingDatasource, setEditingDatasource] = useState<DatasourceRecord | null>(null)
  const [editorState, setEditorState] = useState<DatasourceEditorState | null>(null)
  const [editorSaving, setEditorSaving] = useState(false)
  const [editorError, setEditorError] = useState<string | null>(null)
  const [deletingDatasource, setDeletingDatasource] = useState<DatasourceRecord | null>(null)
  const [deleting, setDeleting] = useState(false)
  const [reloading, setReloading] = useState(false)
  const [flash, setFlash] = useState<FlashMessage | null>(null)
  const { datasources, loading, error, reload } = useDatasources(user?.globalRole)

  const isAdmin = isAdminRole(user?.globalRole)
  const isMainAdmin = isMainAdminRole(user?.globalRole)
  const canManageActiveDatasourceAccess = Boolean(
    activeDatasource &&
      canManageDatasourceAccess(user?.globalRole, user?.id, activeDatasource)
  )
  const selectedDatasource = useMemo(
    () => activeDatasource ?? datasources[0] ?? null,
    [activeDatasource, datasources]
  )

  function openExplorer(datasource: DatasourceRecord) {
    storeSelectedDatasource(toStoredSelection(datasource))
    navigate(`/datasource/${datasource.id}/explorer`)
  }

  async function signOut() {
    await logout()
    clearSavedDatasource()
    navigate('/login', { replace: true })
  }

  async function handleReloadDatasources() {
    try {
      setFlash(null)
      setReloading(true)
      await reload()
      setFlash({
        tone: 'success',
        message: 'Datasources refreshed.',
      })
    } catch {
      setFlash(null)
    } finally {
      setReloading(false)
    }
  }

  function openCreateModal() {
    setFlash(null)
    setCreateError(null)
    setCreatingDatasource(true)
    setCreateState(createEmptyEditorState())
  }

  function closeCreateModal() {
    setCreatingDatasource(false)
    setCreateState(null)
    setCreateError(null)
    setCreateSaving(false)
  }

  async function handleCreateDatasource() {
    if (!createState) {
      return
    }

    try {
      setCreateSaving(true)
      setCreateError(null)

      const payload = buildNewDatasourcePayload(createState)
      await createDataSource(payload)
      await reload()
      closeCreateModal()
      setFlash({
        tone: 'success',
        message: `${payload.displayName} was added successfully.`,
      })
    } catch (creationError) {
      setCreateError(
        creationError instanceof Error
          ? creationError.message
          : 'Saving datasource failed.'
      )
    } finally {
      setCreateSaving(false)
    }
  }

  function openEditModal(datasource: DatasourceRecord) {
    setFlash(null)
    setEditorError(null)
    setEditingDatasource(datasource)
    setEditorState(toEditorState(datasource))
  }

  function closeEditModal() {
    setEditingDatasource(null)
    setEditorState(null)
    setEditorError(null)
    setEditorSaving(false)
  }

  async function handleUpdateDatasource() {
    if (!editingDatasource || !editorState) {
      return
    }

    try {
      setEditorSaving(true)
      setEditorError(null)

      const payload = buildDatasourcePayload(editingDatasource, editorState)
      await updateDataSource(editingDatasource.id, payload)

      const storedDatasource = getStoredDatasourceDetails()
      if (storedDatasource?.id === editingDatasource.id) {
        storeSelectedDatasource({
          ...storedDatasource,
          ...toStoredSelection({
            ...editingDatasource,
            ...payload,
          }),
        })
      }

      await reload()
      closeEditModal()
      setFlash({
        tone: 'success',
        message: `${editingDatasource.displayName} was updated successfully.`,
      })
    } catch (updateError) {
      setEditorError(
        updateError instanceof Error
          ? updateError.message
          : 'Updating datasource failed.'
      )
    } finally {
      setEditorSaving(false)
    }
  }

  async function handleDeleteDatasource() {
    if (!deletingDatasource) {
      return
    }

    try {
      setDeleting(true)
      setFlash(null)
      await deleteDataSource(deletingDatasource.id)

      if (activeDatasource?.id === deletingDatasource.id) {
        setActiveDatasource(null)
      }

      const storedDatasource = getStoredDatasourceDetails()
      if (storedDatasource?.id === deletingDatasource.id) {
        clearSavedDatasource()
      }

      await reload()
      setFlash({
        tone: 'success',
        message: `${deletingDatasource.displayName} was deleted.`,
      })
      setDeletingDatasource(null)
    } catch (deleteError) {
      setFlash({
        tone: 'danger',
        message:
          deleteError instanceof Error
            ? deleteError.message
            : 'Deleting datasource failed.',
      })
    } finally {
      setDeleting(false)
    }
  }

  return (
    <main className="page-shell py-6 sm:py-8">
      <div className="grid gap-6">
        <section className="surface-panel px-6 py-7 sm:px-8 lg:px-10">
          <div className="relative z-10 flex flex-wrap items-start justify-between gap-6">
            <div>
              <span className="section-badge">RBAC workspace</span>
              <h1 className="display-title mt-6 max-w-[12ch] text-[3rem] text-white sm:text-[4rem]">
                Datasource access, clean and visible.
              </h1>
              <p className="display-copy mt-4 max-w-3xl text-sm sm:text-base">
                Review every datasource you can reach, see whether your access is
                manager or viewer, and move straight into the workspace that matters.
              </p>
            </div>

            <div className="flex flex-wrap items-center gap-3">
              {isMainAdmin ? (
                <Button variant="primary" onClick={openCreateModal}>
                  Add datasource
                </Button>
              ) : null}
              <Button
                variant="secondary"
                onClick={() => void handleReloadDatasources()}
                disabled={loading || reloading}
              >
                {reloading ? 'Refreshing...' : 'Refresh datasources'}
              </Button>
              <Button variant="ghost" onClick={() => void signOut()}>
                Log out
              </Button>
            </div>
          </div>
        </section>

        <section className="surface-panel px-6 py-7 sm:px-8">
          <div className="relative z-10">
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div>
                <span className="section-badge">Datasource catalog</span>
                <h2 className="mt-5 text-3xl font-semibold text-white">
                  Accessible datasources
                </h2>
                <p className="mt-3 max-w-2xl text-sm leading-6 text-zinc-300">
                  Each row reflects the access granted to your account. Viewers keep
                  read-only controls, managers can maintain their datasource, and
                  admins can also maintain access assignments.
                </p>
              </div>
            </div>

            {flash ? (
              <div
                className={`mt-5 rounded-[20px] border p-4 text-sm ${flashClassName(flash.tone)}`}
              >
                {flash.message}
              </div>
            ) : null}

            {error ? (
              <div className="mt-5 rounded-[20px] border border-red-400/20 bg-red-500/10 p-4 text-sm text-red-100">
                {error}
              </div>
            ) : null}

            <div className="mt-6 grid gap-4">
              {!loading && datasources.length === 0 ? (
                <div className="rounded-[24px] border border-white/8 bg-white/[0.03] px-6 py-10 text-center text-sm text-zinc-400">
                  <p>No datasources are available for this account yet.</p>
                  {isMainAdmin ? (
                    <div className="mt-5">
                      <Button variant="primary" onClick={openCreateModal}>
                        Add datasource
                      </Button>
                    </div>
                  ) : null}
                </div>
              ) : null}

              {datasources.map((datasource) => {
                const capability = getCapabilityCopy(datasource, isAdmin)
                const canManage = canManageDatasource(user?.globalRole)
                const canManageAccess = canManageDatasourceAccess(
                  user?.globalRole,
                  user?.id,
                  datasource
                )

                return (
                  <article key={datasource.id} className="surface-card p-5">
                    <div className="relative z-10 grid gap-5 lg:grid-cols-[minmax(0,1fr)_15rem] lg:items-center">
                      <div>
                        <div className="flex flex-wrap items-center gap-3">
                          <h3 className="text-xl font-semibold text-white">
                            {datasource.displayName}
                          </h3>
                          <RoleBadge role={datasource.accessRole} />
                        </div>

                        <p className="mt-3 text-sm text-zinc-300">
                          {formatConnection(datasource)}
                        </p>

                        <div className="mt-4 max-w-md">
                          <div className="rounded-[18px] border border-white/8 bg-white/[0.03] p-4">
                            <p className="text-xs uppercase tracking-[0.2em] text-zinc-500">
                              Role state
                            </p>
                            <p className="mt-3 font-semibold text-white">
                              {capability.title}
                            </p>
                            <p className="mt-2 text-sm text-zinc-400">
                              {capability.description}
                            </p>
                          </div>
                        </div>
                      </div>

                      <div className="flex flex-col items-stretch justify-center gap-3 lg:mx-auto lg:w-full">
                        <Button
                          variant="secondary"
                          className="w-full px-4 py-2"
                          onClick={() => openExplorer(datasource)}
                        >
                          Open explorer
                        </Button>
                        {canManage ? (
                          <>
                            <Button
                              variant="secondary"
                              className="w-full px-4 py-2"
                              onClick={openCreateModal}
                            >
                              Add datasource
                            </Button>
                            <Button
                              variant="secondary"
                              className="w-full px-4 py-2"
                              onClick={() => openEditModal(datasource)}
                            >
                              Update datasource
                            </Button>
                            <Button
                              variant="secondary"
                              className="w-full px-4 py-2"
                              onClick={() => {
                                setFlash(null)
                                setDeletingDatasource(datasource)
                              }}
                            >
                              Delete datasource
                            </Button>
                          </>
                        ) : null}
                        {canManageAccess ? (
                          <Button
                            variant="primary"
                            className="w-full px-4 py-2"
                            onClick={() => setActiveDatasource(datasource)}
                          >
                            Manage access
                          </Button>
                        ) : null}
                      </div>
                    </div>
                  </article>
                )
              })}
            </div>
          </div>
        </section>

        <UserManagementPanel enabled={isAdmin} actorRole={user?.globalRole} />
      </div>

      <DatasourceAccessModal
        datasource={selectedDatasource}
        open={canManageActiveDatasourceAccess}
        onClose={() => setActiveDatasource(null)}
      />

      <Modal
        open={creatingDatasource && Boolean(createState)}
        label="Datasource setup"
        title="Add datasource"
        description="Provide the PostgreSQL connection details for the new datasource."
        onClose={closeCreateModal}
      >
        {createState ? (
          <div className="grid gap-4">
            {createError ? (
              <div className="rounded-[18px] border border-red-400/20 bg-red-500/10 p-4 text-sm text-red-100">
                {createError}
              </div>
            ) : null}

            <div className="grid gap-4 md:grid-cols-2">
              <label className="space-y-2 text-sm text-zinc-300">
                <span>Display name</span>
                <input
                  value={createState.displayName}
                  onChange={(event) =>
                    setCreateState({
                      ...createState,
                      displayName: event.target.value,
                    })
                  }
                  placeholder="Production analytics"
                  className="input-shell"
                />
              </label>

              <label className="space-y-2 text-sm text-zinc-300">
                <span>Host</span>
                <input
                  value={createState.host}
                  onChange={(event) =>
                    setCreateState({
                      ...createState,
                      host: event.target.value,
                    })
                  }
                  placeholder="db.internal.example"
                  className="input-shell"
                />
              </label>

              <label className="space-y-2 text-sm text-zinc-300">
                <span>Port</span>
                <input
                  value={createState.port}
                  onChange={(event) =>
                    setCreateState({
                      ...createState,
                      port: event.target.value,
                    })
                  }
                  type="number"
                  className="input-shell"
                />
              </label>

              <label className="space-y-2 text-sm text-zinc-300">
                <span>Database name</span>
                <input
                  value={createState.databaseName}
                  onChange={(event) =>
                    setCreateState({
                      ...createState,
                      databaseName: event.target.value,
                    })
                  }
                  placeholder="warehouse"
                  className="input-shell"
                />
              </label>

              <label className="space-y-2 text-sm text-zinc-300">
                <span>Schema name</span>
                <input
                  value={createState.schemaName}
                  onChange={(event) =>
                    setCreateState({
                      ...createState,
                      schemaName: event.target.value,
                    })
                  }
                  placeholder="public"
                  className="input-shell"
                />
              </label>

              <label className="space-y-2 text-sm text-zinc-300">
                <span>Username</span>
                <input
                  value={createState.username}
                  onChange={(event) =>
                    setCreateState({
                      ...createState,
                      username: event.target.value,
                    })
                  }
                  placeholder="forgeql_app"
                  className="input-shell"
                />
              </label>

              <label className="space-y-2 text-sm text-zinc-300">
                <span>Password</span>
                <input
                  value={createState.password}
                  onChange={(event) =>
                    setCreateState({
                      ...createState,
                      password: event.target.value,
                    })
                  }
                  type="password"
                  placeholder="Enter password"
                  className="input-shell"
                />
              </label>

              <label className="space-y-2 text-sm text-zinc-300">
                <span>SSL mode</span>
                <Dropdown
                  value={createState.sslMode}
                  onChange={(event) =>
                    setCreateState({
                      ...createState,
                      sslMode: event as SslMode,
                      sslRootCertRef: sslModeRequiresCertificate(event as SslMode)
                        ? createState.sslRootCertRef
                        : '',
                    })
                  }
                  options={SSL_MODE_SELECT_OPTIONS}
                  ariaLabel="SSL mode"
                />
              </label>

              <label className="space-y-2 text-sm text-zinc-300">
                <span>Connect timeout (ms)</span>
                <input
                  value={createState.connectTimeoutMs}
                  onChange={(event) =>
                    setCreateState({
                      ...createState,
                      connectTimeoutMs: event.target.value,
                    })
                  }
                  type="number"
                  className="input-shell"
                />
              </label>

              <label className="space-y-2 text-sm text-zinc-300">
                <span>Socket timeout (ms)</span>
                <input
                  value={createState.socketTimeoutMs}
                  onChange={(event) =>
                    setCreateState({
                      ...createState,
                      socketTimeoutMs: event.target.value,
                    })
                  }
                  type="number"
                  className="input-shell"
                />
              </label>

              <label className="space-y-2 text-sm text-zinc-300">
                <span>Application name</span>
                <input
                  value={createState.applicationName}
                  onChange={(event) =>
                    setCreateState({
                      ...createState,
                      applicationName: event.target.value,
                    })
                  }
                  className="input-shell"
                />
              </label>

              {sslModeRequiresCertificate(createState.sslMode) ? (
                <label className="space-y-2 text-sm text-zinc-300">
                  <span>SSL root cert ref</span>
                  <input
                    value={createState.sslRootCertRef}
                    onChange={(event) =>
                      setCreateState({
                        ...createState,
                        sslRootCertRef: event.target.value,
                      })
                    }
                    className="input-shell"
                  />
                </label>
              ) : null}
            </div>

            <label className="space-y-2 text-sm text-zinc-300">
              <span>Extra JDBC options JSON</span>
              <textarea
                value={createState.extraJdbcOptionsJson}
                onChange={(event) =>
                  setCreateState({
                    ...createState,
                    extraJdbcOptionsJson: event.target.value,
                  })
                }
                className="input-shell min-h-[120px] resize-y"
              />
            </label>

            <div className="flex flex-wrap justify-end gap-3">
              <Button variant="ghost" onClick={closeCreateModal} disabled={createSaving}>
                Cancel
              </Button>
              <Button
                variant="primary"
                onClick={() => void handleCreateDatasource()}
                disabled={createSaving}
              >
                {createSaving ? 'Saving...' : 'Create datasource'}
              </Button>
            </div>
          </div>
        ) : null}
      </Modal>

      <Modal
        open={Boolean(editingDatasource && editorState)}
        label="Datasource update"
        title={editingDatasource?.displayName ?? 'Edit datasource'}
        description="Update the connection details for this datasource. Leave the password empty to keep the current one."
        onClose={closeEditModal}
      >
        {editingDatasource && editorState ? (
          <div className="grid gap-4">
            {editorError ? (
              <div className="rounded-[18px] border border-red-400/20 bg-red-500/10 p-4 text-sm text-red-100">
                {editorError}
              </div>
            ) : null}

            <div className="grid gap-4 md:grid-cols-2">
              <label className="space-y-2 text-sm text-zinc-300">
                <span>Display name</span>
                <input
                  value={editorState.displayName}
                  onChange={(event) =>
                    setEditorState({
                      ...editorState,
                      displayName: event.target.value,
                    })
                  }
                  className="input-shell"
                />
              </label>

              <label className="space-y-2 text-sm text-zinc-300">
                <span>Host</span>
                <input
                  value={editorState.host}
                  onChange={(event) =>
                    setEditorState({
                      ...editorState,
                      host: event.target.value,
                    })
                  }
                  className="input-shell"
                />
              </label>

              <label className="space-y-2 text-sm text-zinc-300">
                <span>Port</span>
                <input
                  value={editorState.port}
                  onChange={(event) =>
                    setEditorState({
                      ...editorState,
                      port: event.target.value,
                    })
                  }
                  type="number"
                  className="input-shell"
                />
              </label>

              <label className="space-y-2 text-sm text-zinc-300">
                <span>Database name</span>
                <input
                  value={editorState.databaseName}
                  onChange={(event) =>
                    setEditorState({
                      ...editorState,
                      databaseName: event.target.value,
                    })
                  }
                  className="input-shell"
                />
              </label>

              <label className="space-y-2 text-sm text-zinc-300">
                <span>Schema name</span>
                <input
                  value={editorState.schemaName}
                  onChange={(event) =>
                    setEditorState({
                      ...editorState,
                      schemaName: event.target.value,
                    })
                  }
                  className="input-shell"
                />
              </label>

              <label className="space-y-2 text-sm text-zinc-300">
                <span>Username</span>
                <input
                  value={editorState.username}
                  onChange={(event) =>
                    setEditorState({
                      ...editorState,
                      username: event.target.value,
                    })
                  }
                  className="input-shell"
                />
              </label>

              <label className="space-y-2 text-sm text-zinc-300">
                <span>New password</span>
                <input
                  value={editorState.password}
                  onChange={(event) =>
                    setEditorState({
                      ...editorState,
                      password: event.target.value,
                    })
                  }
                  type="password"
                  placeholder="Keep current password"
                  className="input-shell"
                />
              </label>

              <label className="space-y-2 text-sm text-zinc-300">
                <span>SSL mode</span>
                <Dropdown
                  value={editorState.sslMode}
                  onChange={(event) =>
                    setEditorState({
                      ...editorState,
                      sslMode: event as SslMode,
                      sslRootCertRef: sslModeRequiresCertificate(event as SslMode)
                        ? editorState.sslRootCertRef
                        : '',
                    })
                  }
                  options={SSL_MODE_SELECT_OPTIONS}
                  ariaLabel="SSL mode"
                />
              </label>

              <label className="space-y-2 text-sm text-zinc-300">
                <span>Connect timeout (ms)</span>
                <input
                  value={editorState.connectTimeoutMs}
                  onChange={(event) =>
                    setEditorState({
                      ...editorState,
                      connectTimeoutMs: event.target.value,
                    })
                  }
                  type="number"
                  className="input-shell"
                />
              </label>

              <label className="space-y-2 text-sm text-zinc-300">
                <span>Socket timeout (ms)</span>
                <input
                  value={editorState.socketTimeoutMs}
                  onChange={(event) =>
                    setEditorState({
                      ...editorState,
                      socketTimeoutMs: event.target.value,
                    })
                  }
                  type="number"
                  className="input-shell"
                />
              </label>

              <label className="space-y-2 text-sm text-zinc-300">
                <span>Application name</span>
                <input
                  value={editorState.applicationName}
                  onChange={(event) =>
                    setEditorState({
                      ...editorState,
                      applicationName: event.target.value,
                    })
                  }
                  className="input-shell"
                />
              </label>

              {sslModeRequiresCertificate(editorState.sslMode) ? (
                <label className="space-y-2 text-sm text-zinc-300">
                  <span>SSL root cert ref</span>
                  <input
                    value={editorState.sslRootCertRef}
                    onChange={(event) =>
                      setEditorState({
                        ...editorState,
                        sslRootCertRef: event.target.value,
                      })
                    }
                    className="input-shell"
                  />
                </label>
              ) : null}
            </div>

            <label className="space-y-2 text-sm text-zinc-300">
              <span>Extra JDBC options JSON</span>
              <textarea
                value={editorState.extraJdbcOptionsJson}
                onChange={(event) =>
                  setEditorState({
                    ...editorState,
                    extraJdbcOptionsJson: event.target.value,
                  })
                }
                className="input-shell min-h-[120px] resize-y"
              />
            </label>

            <div className="flex flex-wrap justify-end gap-3">
              <Button variant="ghost" onClick={closeEditModal} disabled={editorSaving}>
                Cancel
              </Button>
              <Button
                variant="primary"
                onClick={() => void handleUpdateDatasource()}
                disabled={editorSaving}
              >
                {editorSaving ? 'Updating...' : 'Update datasource'}
              </Button>
            </div>
          </div>
        ) : null}
      </Modal>

      <Modal
        open={Boolean(deletingDatasource)}
        label="Datasource deletion"
        title={deletingDatasource ? `Delete ${deletingDatasource.displayName}?` : 'Delete datasource'}
        description="This removes the datasource record and clears its generated schema snapshot."
        onClose={() => {
          if (!deleting) {
            setDeletingDatasource(null)
          }
        }}
      >
        {deletingDatasource ? (
          <div className="grid gap-5">
            <div className="rounded-[18px] border border-red-400/20 bg-red-500/10 p-4 text-sm leading-6 text-red-100">
              Deleting this datasource will remove it from the workspace immediately.
            </div>

            <div className="rounded-[18px] border border-white/8 bg-white/[0.03] p-4">
              <p className="text-xs uppercase tracking-[0.2em] text-zinc-500">
                Connection
              </p>
              <p className="mt-3 text-sm text-zinc-100">
                {formatConnection(deletingDatasource)}
              </p>
            </div>

            <div className="flex flex-wrap justify-end gap-3">
              <Button
                variant="ghost"
                onClick={() => setDeletingDatasource(null)}
                disabled={deleting}
              >
                Cancel
              </Button>
              <Button
                variant="danger"
                onClick={() => void handleDeleteDatasource()}
                disabled={deleting}
              >
                {deleting ? 'Deleting...' : 'Delete datasource'}
              </Button>
            </div>
          </div>
        ) : null}
      </Modal>
    </main>
  )
}
