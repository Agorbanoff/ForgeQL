import { useCallback, useEffect, useState } from 'react'
import { getAdminUsers, updateUserRole } from '../services/api'
import type { AdminUserRecord, GlobalRole } from '../types/platform'

export function useUserManagement(enabled: boolean) {
  const [users, setUsers] = useState<AdminUserRecord[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [savingUserId, setSavingUserId] = useState<number | null>(null)

  const reload = useCallback(async () => {
    if (!enabled) {
      setUsers([])
      setLoading(false)
      setError(null)
      return
    }

    try {
      setLoading(true)
      setError(null)
      setUsers(await getAdminUsers())
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Could not load users.')
    } finally {
      setLoading(false)
    }
  }, [enabled])

  useEffect(() => {
    void reload()
  }, [reload])

  const saveRole = useCallback(
    async (userId: number, globalRole: GlobalRole) => {
      try {
        setSavingUserId(userId)
        await updateUserRole(userId, globalRole)
        await reload()
      } finally {
        setSavingUserId(null)
      }
    },
    [reload]
  )

  return {
    users,
    loading,
    error,
    reload,
    saveRole,
    savingUserId,
  }
}
