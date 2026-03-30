import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { signUpUser } from '../api/accountApi'
import { clearSavedDatasource, markSessionActive } from '../lib/appState'

export default function SignUpPage() {
  const navigate = useNavigate()

  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')

  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()

    setError(null)
    setSuccess(null)

    if (!username.trim()) {
      setError('Username is required.')
      return
    }

    if (!email.trim()) {
      setError('Identifier is required.')
      return
    }

    if (!password) {
      setError('Password is required.')
      return
    }

    if (password !== confirmPassword) {
      setError('Passwords do not match.')
      return
    }

    try {
      setLoading(true)

      await signUpUser({
        username: username.trim(),
        email: email.trim(),
        password,
      })

      clearSavedDatasource()
      markSessionActive()
      setSuccess('Account created successfully.')
      setUsername('')
      setEmail('')
      setPassword('')
      setConfirmPassword('')

      setTimeout(() => {
        navigate('/connection-request')
      }, 800)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Sign up failed.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="flex min-h-[calc(100vh-80px)] items-center justify-center">
      <div className="w-full max-w-md rounded-xl border border-zinc-800 bg-zinc-900 p-6">
        <h2 className="mb-4 text-2xl font-semibold">Sign up</h2>

        <form className="space-y-4" onSubmit={handleSubmit}>
          <input
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="Username"
            className="w-full rounded-lg border border-zinc-700 bg-zinc-800 p-3 outline-none"
          />

          <input
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            type="text"
            placeholder="Email or any identifier"
            className="w-full rounded-lg border border-zinc-700 bg-zinc-800 p-3 outline-none"
          />

          <p className="text-xs text-zinc-500">
            Simple identifiers are converted to a backend-safe email automatically.
          </p>

          <input
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            type="password"
            placeholder="Password"
            className="w-full rounded-lg border border-zinc-700 bg-zinc-800 p-3 outline-none"
          />

          <p className="text-xs text-zinc-500">
            If the password is shorter than 8 characters, the frontend pads it consistently for you.
          </p>

          <input
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            type="password"
            placeholder="Confirm password"
            className="w-full rounded-lg border border-zinc-700 bg-zinc-800 p-3 outline-none"
          />

          {error && (
            <div className="rounded-lg border border-red-800 bg-red-950/40 p-3 text-sm text-red-300">
              {error}
            </div>
          )}

          {success && (
            <div className="rounded-lg border border-emerald-800 bg-emerald-950/40 p-3 text-sm text-emerald-300">
              {success}
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full rounded-lg bg-cyan-500 p-3 text-black disabled:cursor-not-allowed disabled:opacity-60"
          >
            {loading ? 'Signing up...' : 'Sign up'}
          </button>
        </form>

        <p className="mt-4 text-sm text-zinc-400">
          Already have account?{' '}
          <Link to="/login" className="text-cyan-400">
            Log in
          </Link>
        </p>
      </div>
    </main>
  )
}
