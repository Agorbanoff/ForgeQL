import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { logInUser } from '../api/accountApi'
import { clearSavedDatasource, markSessionActive } from '../lib/appState'
import AuthLayout from '../components/AuthLayout'

export default function LoginPage() {
  const navigate = useNavigate()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()

    setError(null)
    setSuccess(null)

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

      await logInUser({
        email: email.trim(),
        password,
      })

      clearSavedDatasource()
      markSessionActive()
      setSuccess('Logged in successfully. Redirecting to connection setup...')
      setEmail('')
      setPassword('')

      setTimeout(() => {
        navigate('/connection-request')
      }, 800)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Log in failed.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthLayout
      badge="Elegant Access"
      title="Smarter queries. Faster answers."
      description="Enter the SigmaQL workspace through a calmer, more premium interface built around secure access, structured querying, and smooth transitions from auth to execution."
      formTitle="Log in"
      formDescription="Log in with the exact email and password you registered with."
      form={
        <form className="space-y-4" onSubmit={handleSubmit}>
          <div className="space-y-2">
            <label className="text-sm font-medium text-zinc-300">Email</label>
            <input
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              type="email"
              placeholder="name@example.com"
              className="input-shell"
            />
          </div>

          <div className="space-y-2">
            <label className="text-sm font-medium text-zinc-300">Password</label>
            <input
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              type="password"
              placeholder="Password"
              className="input-shell"
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
            className="primary-button mt-2 w-full"
            data-pressable
          >
            {loading ? 'Logging in...' : 'Enter workspace'}
          </button>
        </form>
      }
      footer={
        <p>
          No account yet?{' '}
          <Link to="/signup" className="text-cyan-300" data-pressable>
            Create one here
          </Link>
          .
        </p>
      }
    />
  )
}
