import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthProvider'
import { useDatasources } from '../hooks/useDatasources'
import { clearSavedDatasource, storeSelectedDatasource } from '../lib/appState'
import type { DatasourceRecord } from '../types/platform'
import { DatasourceAccessModal } from '../components/rbac/DatasourceAccessModal'
import { UserManagementPanel } from '../components/rbac/UserManagementPanel'
import { RoleBadge } from '../components/rbac/RoleBadge'
import { Button } from '../components/ui/Button'

function formatConnection(datasource: DatasourceRecord) {
  return `${datasource.host}:${datasource.port} • ${datasource.databaseName}/${datasource.schemaName}`
}

function formatUpdatedAt(value: string) {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

function getCapabilityCopy(datasource: DatasourceRecord, isAdmin: boolean) {
  if (isAdmin) {
    return {
      title: 'Full control',
      description: 'Open the explorer and manage datasource access from here.',
    }
  }

  if (datasource.accessRole === 'MANAGER') {
    return {
      title: 'Manager access',
      description: 'You can work with this datasource and handle manager-level flows.',
    }
  }

  return {
    title: 'Read only',
    description: 'This datasource is visible, but management actions stay locked.',
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

export default function ConnectionRequestPage() {
  const navigate = useNavigate()
  const { user, logout } = useAuth()
  const [activeDatasource, setActiveDatasource] = useState<DatasourceRecord | null>(null)
  const { datasources, loading, error, reload, summary } = useDatasources(
    user?.globalRole
  )

  const isAdmin = user?.globalRole === 'ADMIN'
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
                manager or viewer, and handle admin-only role assignment from one
                place.
              </p>
            </div>

            <div className="flex flex-wrap items-center gap-3">
              <div className="rounded-[22px] border border-white/8 bg-white/[0.03] px-4 py-3">
                <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">
                  Signed in as
                </p>
                <p className="mt-2 text-sm font-semibold text-white">
                  {user?.username ?? 'Unknown user'}
                </p>
                <div className="mt-3">
                  {user ? <RoleBadge role={user.globalRole} /> : null}
                </div>
              </div>
              <Button variant="secondary" onClick={() => void reload()}>
                Refresh datasources
              </Button>
              <Button variant="ghost" onClick={() => void signOut()}>
                Log out
              </Button>
            </div>
          </div>

          <div className="mt-8 grid gap-4 md:grid-cols-3">
            <div className="surface-card p-5">
              <div className="relative z-10">
                <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">
                  Total datasources
                </p>
                <p className="stat-value mt-3 text-5xl text-white">
                  {loading ? '...' : summary.total}
                </p>
              </div>
            </div>
            <div className="surface-card p-5">
              <div className="relative z-10">
                <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">
                  Manageable
                </p>
                <p className="stat-value mt-3 text-5xl text-white">
                  {loading ? '...' : summary.manageableCount}
                </p>
              </div>
            </div>
            <div className="surface-card p-5">
              <div className="relative z-10">
                <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">
                  Read only
                </p>
                <p className="stat-value mt-3 text-5xl text-white">
                  {loading ? '...' : summary.readOnlyCount}
                </p>
              </div>
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
                  read-only controls, managers can work with their datasource, and
                  admins can also maintain access assignments.
                </p>
              </div>
            </div>

            {error ? (
              <div className="mt-5 rounded-[20px] border border-red-400/20 bg-red-500/10 p-4 text-sm text-red-100">
                {error}
              </div>
            ) : null}

            <div className="mt-6 grid gap-4">
              {!loading && datasources.length === 0 ? (
                <div className="rounded-[24px] border border-white/8 bg-white/[0.03] px-6 py-10 text-center text-sm text-zinc-400">
                  No datasources are available for this account yet.
                </div>
              ) : null}

              {datasources.map((datasource) => {
                const capability = getCapabilityCopy(datasource, isAdmin)
                const canManage = isAdmin || datasource.accessRole === 'MANAGER'

                return (
                  <article key={datasource.id} className="surface-card p-5">
                    <div className="relative z-10 grid gap-5 lg:grid-cols-[minmax(0,1fr)_auto] lg:items-start">
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

                        <div className="mt-4 grid gap-3 sm:grid-cols-3">
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
                          <div className="rounded-[18px] border border-white/8 bg-white/[0.03] p-4">
                            <p className="text-xs uppercase tracking-[0.2em] text-zinc-500">
                              Server
                            </p>
                            <p className="mt-3 font-semibold text-white">
                              {datasource.serverVersion ?? 'Unknown'}
                            </p>
                            <p className="mt-2 text-sm text-zinc-400">
                              {datasource.dbType}
                            </p>
                          </div>
                          <div className="rounded-[18px] border border-white/8 bg-white/[0.03] p-4">
                            <p className="text-xs uppercase tracking-[0.2em] text-zinc-500">
                              Updated
                            </p>
                            <p className="mt-3 font-semibold text-white">
                              {formatUpdatedAt(datasource.updatedAt)}
                            </p>
                            <p className="mt-2 text-sm text-zinc-400">
                              Owner #{datasource.ownerUserId}
                            </p>
                          </div>
                        </div>
                      </div>

                      <div className="flex flex-wrap gap-3 lg:w-[19rem] lg:justify-end">
                        <Button
                          variant="secondary"
                          className="px-4 py-2"
                          onClick={() => openExplorer(datasource)}
                        >
                          Open explorer
                        </Button>
                        <Button
                          variant="secondary"
                          className="px-4 py-2"
                          disabled={!canManage}
                        >
                          Edit datasource
                        </Button>
                        <Button
                          variant="secondary"
                          className="px-4 py-2"
                          disabled={!canManage}
                        >
                          Delete datasource
                        </Button>
                        {isAdmin ? (
                          <Button
                            variant="primary"
                            className="px-4 py-2"
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

        <UserManagementPanel enabled={isAdmin} />
      </div>

      <DatasourceAccessModal
        datasource={selectedDatasource}
        open={Boolean(isAdmin && activeDatasource)}
        onClose={() => setActiveDatasource(null)}
      />
    </main>
  )
}
