import { useState } from 'react'
import { useUserManagement } from '../../hooks/useUserManagement'
import type { GlobalRole } from '../../types/platform'
import { Dropdown } from '../ui/Dropdown'
import { Table } from '../ui/Table'
import { Button } from '../ui/Button'
import { RoleBadge } from './RoleBadge'

const ROLE_OPTIONS = [
  {
    value: 'ADMIN',
    label: 'Admin',
    description: 'Full control over all datasources and users.',
  },
  {
    value: 'MEMBER',
    label: 'Member',
    description: 'Standard user with managed datasource access.',
  },
  {
    value: 'VIEWER',
    label: 'Viewer',
    description: 'Read-only user across permitted datasources.',
  },
] as const

export function UserManagementPanel({ enabled }: { enabled: boolean }) {
  const [draftRoles, setDraftRoles] = useState<Record<number, GlobalRole>>({})
  const { users, loading, error, saveRole, savingUserId, reload } =
    useUserManagement(enabled)

  if (!enabled) {
    return null
  }

  return (
    <section className="surface-panel px-6 py-7 sm:px-8">
      <div className="relative z-10">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <span className="section-badge">Admin only</span>
            <h2 className="mt-5 text-3xl font-semibold text-white">
              User management
            </h2>
            <p className="mt-3 max-w-2xl text-sm leading-6 text-zinc-300">
              Update global roles for the workspace. Viewers remain read-only,
              members can be granted datasource management, and admins control the
              whole system.
            </p>
          </div>
          <Button variant="secondary" onClick={() => void reload()}>
            Refresh users
          </Button>
        </div>

        {error ? (
          <div className="mt-5 rounded-[20px] border border-red-400/20 bg-red-500/10 p-4 text-sm text-red-100">
            {error}
          </div>
        ) : null}

        <div className="mt-6">
          <Table
            rows={users.map((user) => ({
              ...user,
              selectedRole: draftRoles[user.id] ?? user.globalRole,
            }))}
            getRowKey={(user) => user.id}
            emptyState={
              <div className="px-2 py-8 text-center text-sm text-zinc-400">
                {loading ? 'Loading users...' : 'No users found.'}
              </div>
            }
            columns={[
              {
                key: 'identity',
                header: 'User',
                render: (user) => (
                  <div>
                    <p className="font-semibold text-white">{user.username}</p>
                    <p className="mt-1 text-xs text-zinc-400">{user.email}</p>
                  </div>
                ),
              },
              {
                key: 'current-role',
                header: 'Current role',
                render: (user) => <RoleBadge role={user.globalRole} />,
              },
              {
                key: 'new-role',
                header: 'Global role',
                render: (user) => (
                  <Dropdown
                    ariaLabel={`Global role for ${user.username}`}
                    value={user.selectedRole}
                    options={ROLE_OPTIONS}
                    onChange={(value) =>
                      setDraftRoles((current) => ({
                        ...current,
                        [user.id]: value as GlobalRole,
                      }))
                    }
                    disabled={savingUserId === user.id}
                  />
                ),
              },
              {
                key: 'actions',
                header: 'Actions',
                className: 'w-[11rem]',
                render: (user) => (
                  <Button
                    variant="primary"
                    className="px-4 py-2"
                    disabled={
                      savingUserId === user.id ||
                      user.selectedRole === user.globalRole
                    }
                    onClick={() => void saveRole(user.id, user.selectedRole)}
                  >
                    {savingUserId === user.id ? 'Saving...' : 'Save'}
                  </Button>
                ),
              },
            ]}
          />
        </div>
      </div>
    </section>
  )
}
