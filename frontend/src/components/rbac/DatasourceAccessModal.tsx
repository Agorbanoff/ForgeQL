import { useMemo, useState } from 'react'
import { useDatasourceAccessManagement } from '../../hooks/useDatasourceAccessManagement'
import type {
  DatasourceAccessRole,
  DatasourceRecord,
} from '../../types/platform'
import { Button } from '../ui/Button'
import { Dropdown } from '../ui/Dropdown'
import { Modal } from '../ui/Modal'
import { Table } from '../ui/Table'
import { RoleBadge } from './RoleBadge'

const ACCESS_OPTIONS = [
  {
    value: 'MANAGER',
    label: 'Manager',
    description: 'Can manage and edit this datasource.',
  },
  {
    value: 'VIEWER',
    label: 'Viewer',
    description: 'Read-only access to this datasource.',
  },
] as const

export function DatasourceAccessModal({
  datasource,
  open,
  onClose,
}: {
  datasource: DatasourceRecord | null
  open: boolean
  onClose: () => void
}) {
  const [draftRoles, setDraftRoles] = useState<Record<number, DatasourceAccessRole>>({})
  const [newUserId, setNewUserId] = useState('')
  const [newAccessRole, setNewAccessRole] = useState<DatasourceAccessRole>('VIEWER')
  const [localError, setLocalError] = useState<string | null>(null)

  const {
    accessList,
    loading,
    error,
    saveRole,
    removeAccess,
    assignAccess,
    busyUserId,
    assigning,
    actionError,
  } = useDatasourceAccessManagement(datasource?.id ?? null, open)

  const rows = useMemo(
    () =>
      accessList
        .filter((record) => record.globalRole !== 'MAIN_ADMIN')
        .map((record) => ({
          ...record,
          selectedRole: draftRoles[record.userId] ?? record.accessRole,
        })),
    [accessList, draftRoles]
  )

  async function handleAssign() {
    const parsedUserId = Number(newUserId)

    if (!Number.isInteger(parsedUserId) || parsedUserId < 1) {
      setLocalError('Enter a valid numeric user id before assigning access.')
      return
    }

    setLocalError(null)

    try {
      await assignAccess(parsedUserId, newAccessRole)
      setNewUserId('')
      setNewAccessRole('VIEWER')
    } catch (error) {
      setLocalError(
        error instanceof Error ? error.message : 'Assigning access failed.'
      )
    }
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={datasource ? `${datasource.displayName}` : 'Datasource access'}
      description="Review who can work with this datasource, update datasource-level roles, and grant new access without leaving the page."
    >
      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_20rem]">
        <section className="space-y-4">
          {error ? (
            <div className="rounded-[20px] border border-red-400/20 bg-red-500/10 p-4 text-sm text-red-100">
              {error}
            </div>
          ) : null}

          {actionError ? (
            <div className="rounded-[20px] border border-amber-400/20 bg-amber-500/10 p-4 text-sm text-amber-100">
              {actionError}
            </div>
          ) : null}

          <Table
            rows={rows}
            getRowKey={(row) => row.userId}
            emptyState={
              <div className="px-2 py-8 text-center text-sm text-zinc-400">
                {loading ? 'Loading access list...' : 'No users have access yet.'}
              </div>
            }
            columns={[
              {
                key: 'user',
                header: 'User',
                render: (row) => (
                  <div>
                    <p className="font-semibold text-white">{row.username}</p>
                    <p className="mt-1 text-xs text-zinc-400">{row.email}</p>
                  </div>
                ),
              },
              {
                key: 'current-role',
                header: 'Current role',
                render: (row) => <RoleBadge role={row.accessRole} />,
              },
              {
                key: 'change-role',
                header: 'Role',
                render: (row) => (
                  <Dropdown
                    ariaLabel={`Role for ${row.username}`}
                    value={row.selectedRole}
                    options={ACCESS_OPTIONS}
                    onChange={(value) =>
                      setDraftRoles((current) => ({
                        ...current,
                        [row.userId]: value as DatasourceAccessRole,
                      }))
                    }
                    disabled={busyUserId === row.userId}
                  />
                ),
              },
              {
                key: 'actions',
                header: 'Actions',
                className: 'w-[15rem]',
                render: (row) => (
                  <div className="flex flex-wrap gap-2">
                    <Button
                      variant="primary"
                      className="px-4 py-2"
                      disabled={
                        busyUserId === row.userId ||
                        row.selectedRole === row.accessRole
                      }
                      onClick={() => void saveRole(row.userId, row.selectedRole)}
                    >
                      {busyUserId === row.userId ? 'Saving...' : 'Update'}
                    </Button>
                    <Button
                      variant="danger"
                      className="px-4 py-2"
                      disabled={busyUserId === row.userId}
                      onClick={() => void removeAccess(row.userId)}
                    >
                      Remove
                    </Button>
                  </div>
                ),
              },
            ]}
          />
        </section>

        <aside className="surface-card p-5">
          <div className="relative z-10">
            <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">
              Assign access
            </p>
            <h3 className="mt-3 text-xl font-semibold text-white">Add a user</h3>
            <p className="mt-3 text-sm leading-6 text-zinc-300">
              Use a numeric user id from the admin user directory, then choose the
              datasource role to apply.
            </p>
            <p className="mt-3 text-xs leading-6 text-amber-100/85">
              Admin and main admin accounts do not receive datasource-level access.
            </p>

            <div className="mt-5 space-y-4">
              <div className="space-y-2">
                <label className="text-sm font-medium text-zinc-300">User id</label>
                <input
                  value={newUserId}
                  onChange={(event) => setNewUserId(event.target.value)}
                  className="input-shell"
                  placeholder="42"
                  inputMode="numeric"
                />
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium text-zinc-300">Access role</label>
                <Dropdown
                  ariaLabel="New datasource access role"
                  value={newAccessRole}
                  options={ACCESS_OPTIONS}
                  onChange={(value) =>
                    setNewAccessRole(value as DatasourceAccessRole)
                  }
                />
              </div>
            </div>

            {localError ? (
              <div className="mt-4 rounded-[18px] border border-red-400/20 bg-red-500/10 p-3 text-sm text-red-100">
                {localError}
              </div>
            ) : null}

            <Button
              variant="primary"
              className="mt-5 w-full"
              disabled={assigning}
              onClick={() => void handleAssign()}
            >
              {assigning ? 'Assigning...' : 'Assign access'}
            </Button>
          </div>
        </aside>
      </div>
    </Modal>
  )
}
