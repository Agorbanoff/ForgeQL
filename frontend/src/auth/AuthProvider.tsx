import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import {
  getCurrentUser,
  logInUser,
  logOutUser,
  type CurrentUser,
  type LogInPayload,
} from '../api/accountApi'
import { AUTH_REQUIRED_EVENT, ApiRequestError } from '../api/http'
import { clearSavedDatasource } from '../lib/appState'

type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated'

type AuthContextValue = {
  status: AuthStatus
  user: CurrentUser | null
  refreshSession: () => Promise<CurrentUser | null>
  login: (payload: LogInPayload) => Promise<CurrentUser>
  logout: (options?: { skipRequest?: boolean }) => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

function isAuthError(error: unknown) {
  return error instanceof ApiRequestError && error.status === 401
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>('loading')
  const [user, setUser] = useState<CurrentUser | null>(null)

  async function refreshSession(): Promise<CurrentUser | null> {
    try {
      const currentUser = await getCurrentUser()
      setUser(currentUser)
      setStatus('authenticated')
      return currentUser
    } catch (error) {
      if (isAuthError(error)) {
        clearSavedDatasource()
        setUser(null)
        setStatus('unauthenticated')
        return null
      }

      clearSavedDatasource()
      setUser(null)
      setStatus('unauthenticated')
      throw error
    }
  }

  async function login(payload: LogInPayload) {
    await logInUser(payload)
    const currentUser = await refreshSession()

    if (!currentUser) {
      throw new Error('Login succeeded, but the session could not be restored.')
    }

    return currentUser
  }

  async function logout(options: { skipRequest?: boolean } = {}) {
    if (!options.skipRequest) {
      try {
        await logOutUser()
      } catch {
        // Local auth cleanup still runs if the backend session already expired.
      }
    }

    clearSavedDatasource()
    setUser(null)
    setStatus('unauthenticated')
  }

  useEffect(() => {
    void refreshSession().catch(() => undefined)
  }, [])

  useEffect(() => {
    function handleAuthRequired() {
      void logout({ skipRequest: true })
    }

    window.addEventListener(AUTH_REQUIRED_EVENT, handleAuthRequired)

    return () => {
      window.removeEventListener(AUTH_REQUIRED_EVENT, handleAuthRequired)
    }
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      status,
      user,
      refreshSession,
      login,
      logout,
    }),
    [status, user]
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)

  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider.')
  }

  return context
}
