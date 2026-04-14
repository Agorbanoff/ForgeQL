import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthProvider'
import AuthLayout from '../components/AuthLayout'
import { clearSavedDatasource } from '../lib/appState'

type LoginLocationState = {
  email?: string
  password?: string
  fromSignup?: boolean
}

export default function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const { login } = useAuth()
  const locationState = (location.state ?? null) as LoginLocationState | null

  const [email, setEmail] = useState(locationState?.email ?? '')
  const [password, setPassword] = useState(locationState?.password ?? '')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const success = locationState?.fromSignup
    ? 'Account created. Log in to continue into datasource management.'
    : null

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    setError(null)

    if (!email.trim()) {
      setError('Email is required.')
      return
    }

    if (!password) {
      setError('Password is required.')
      return
    }

    try {
      setLoading(true)

      await login({
        email: email.trim(),
        password,
      })

      clearSavedDatasource()
      navigate('/datasource', { replace: true })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Log in failed.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthLayout
      badge="Step 1 of 3"
      title="Log in before the workspace opens."
      description="Authentication opens the secured console. After login succeeds the app moves directly into datasource management, and the session cookies are issued by the backend."
      formTitle="Log in"
      formDescription="Use the same email and password that belong to the account you created."
      highlights={[
        'Access stays locked until login succeeds.',
        'Datasource setup is always the next step after authentication.',
        'Access and refresh JWT cookies are created after a successful sign in.',
      ]}
      form={
        <form className="space-y-3.5" onSubmit={handleSubmit}>
          <div className="space-y-1.5">
            <label className="text-sm font-medium text-zinc-300">Email</label>
            <input
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              type="email"
              placeholder="name@example.com"
              className="input-shell"
              autoComplete="email"
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-sm font-medium text-zinc-300">Password</label>
            <input
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              type="password"
              placeholder="Password"
              className="input-shell"
              autoComplete="current-password"
            />
          </div>

          {error && (
            <div className="rounded-[20px] border border-red-500/20 bg-red-500/8 p-4 text-sm text-red-200">
              {error}
            </div>
          )}

          {success && (
            <div className="rounded-[20px] border border-emerald-400/20 bg-emerald-400/8 p-4 text-sm text-emerald-200">
              {success}
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            className="primary-button mt-1.5 w-full"
            data-pressable
            data-glow={!loading ? 'pulse' : undefined}
          >
            {loading ? 'Logging in...' : 'Continue'}
          </button>
        </form>
      }
      footer={
        <p>
          No account yet?{' '}
          <Link to="/signup" className="auth-footer-link" data-pressable>
            Create one here
          </Link>
          .
        </p>
      }
    />
  )
}
