import { useCallback, useEffect, useState } from 'react'
import {
  assignDatasourceAccess,
  deleteDatasourceAccess,
  getDatasourceAccessList,
  updateDatasourceAccess,
} from '../services/api'
import type {
  DatasourceAccessRecord,
  DatasourceAccessRole,
} from '../types/platform'

type State = {
  records: DatasourceAccessRecord[]
  loading: boolean
  error: string | null
}

export function useDatasourceAccessManagement(
  datasourceId: number | null,
  enabled: boolean
) {
  const [state, setState] = useState<State>({
    records: [],
    loading: false,
    error: null,
  })
  const [busyUserId, setBusyUserId] = useState<number | null>(null)
  const [assigning, setAssigning] = useState(false)

  const reload = useCallback(async () => {
    if (!datasourceId || !enabled) {
      setState({ records: [], loading: false, error: null })
      return
    }

    try {
      setState((current) => ({ ...current, loading: true, error: null }))
      const records = await getDatasourceAccessList(datasourceId)
      setState({ records, loading: false, error: null })
    } catch (error) {
      setState({
        records: [],
        loading: false,
        error:
          error instanceof Error
            ? error.message
            : 'Could not load datasource access.',
      })
    }
  }, [datasourceId, enabled])

  useEffect(() => {
    void reload()
  }, [reload])

  const saveRole = useCallback(
    async (userId: number, accessRole: DatasourceAccessRole) => {
      if (!datasourceId) {
        return
      }

      try {
        setBusyUserId(userId)
        await updateDatasourceAccess(datasourceId, userId, { accessRole })
        await reload()
      } finally {
        setBusyUserId(null)
      }
    },
    [datasourceId, reload]
  )

  const removeAccess = useCallback(
    async (userId: number) => {
      if (!datasourceId) {
        return
      }

      try {
        setBusyUserId(userId)
        await deleteDatasourceAccess(datasourceId, userId)
        await reload()
      } finally {
        setBusyUserId(null)
      }
    },
    [datasourceId, reload]
  )

  const assignAccess = useCallback(
    async (userId: number, accessRole: DatasourceAccessRole) => {
      if (!datasourceId) {
        return
      }

      try {
        setAssigning(true)
        await assignDatasourceAccess(datasourceId, { userId, accessRole })
        await reload()
      } finally {
        setAssigning(false)
      }
    },
    [datasourceId, reload]
  )

  return {
    accessList: state.records,
    loading: state.loading,
    error: state.error,
    reload,
    saveRole,
    removeAccess,
    assignAccess,
    busyUserId,
    assigning,
  }
}
