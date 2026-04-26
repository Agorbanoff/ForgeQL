import { useCallback, useEffect, useMemo, useState } from 'react'
import { getDatasources } from '../services/api'
import type { DatasourceRecord, GlobalRole } from '../types/platform'

function sortDatasources(items: DatasourceRecord[]) {
  return [...items].sort((left, right) =>
    left.displayName.localeCompare(right.displayName)
  )
}

export function useDatasources(currentUserRole?: GlobalRole) {
  const [datasources, setDatasources] = useState<DatasourceRecord[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const reload = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const nextDatasources = await getDatasources()
      setDatasources(sortDatasources(nextDatasources))
    } catch (error) {
      setError(
        error instanceof Error ? error.message : 'Could not load datasources.'
      )
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void reload()
  }, [reload])

  const summary = useMemo(() => {
    const manageableCount = datasources.filter((datasource) => {
      if (currentUserRole === 'ADMIN') {
        return true
      }

      return datasource.accessRole === 'MANAGER'
    }).length

    return {
      total: datasources.length,
      manageableCount,
      readOnlyCount: datasources.length - manageableCount,
    }
  }, [currentUserRole, datasources])

  return {
    datasources,
    loading,
    error,
    reload,
    summary,
  }
}
