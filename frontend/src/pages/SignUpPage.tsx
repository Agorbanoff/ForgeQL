import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { signUpUser } from '../api/accountApi'
import AuthLayout from '../components/AuthLayout'
import { clearSavedDatasource } from '../lib/appState'

export default function SignUpPage() {
  const navigate = useNavigate()

  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    setError(null)

    if (!username.trim()) {
      setError('Username is required.')
      return
    }

    if (!email.trim()) {
      setError('Email is required.')
      return
    }

    if (!password) {
      setError('Password is required.')
      return
    }

    if (password.length < 8) {
      setError('Password must be at least 8 characters long.')
      return
    }

    if (password !== confirmPassword) {
      setError('Passwords do not match.')
      return
    }

    const trimmedUsername = username.trim()
    const trimmedEmail = email.trim()

    try {
      setLoading(true)

      await signUpUser({
        username: trimmedUsername,
        email: trimmedEmail,
        password,
      })

      clearSavedDatasource()
      navigate('/login', {
        replace: true,
        state: {
          email: trimmedEmail,
          password,
          fromSignup: true,
        },
      })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Sign up failed.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthLayout
      badge="Create account"
      title="Start with a clean account setup."
      description="Create the client account first. After that, the flow moves to login so tokens are generated only when the user explicitly signs in."
      formTitle="Create account"
      formDescription="Use a real email and password. Once the account is created, the login form will already be filled for you."
      highlights={[
        'This page only creates the account and does not open the workspace yet.',
        'Login remains the required step for issuing the JWT cookies.',
        'Your email and password are carried into the next screen automatically.',
      ]}
      form={
        <form className="space-y-4" onSubmit={handleSubmit}>
          <div className="space-y-2">
            <label className="text-sm font-medium text-zinc-300">Username</label>
            <input
              value={username}
              onChange={(event) => setUsername(event.target.value)}
              placeholder="Username"
              className="input-shell"
              autoComplete="username"
            />
          </div>

          <div className="space-y-2">
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

          <div className="space-y-2">
            <label className="text-sm font-medium text-zinc-300">Password</label>
            <input
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              type="password"
              placeholder="Password"
              className="input-shell"
              autoComplete="new-password"
            />
          </div>

          <div className="space-y-2">
            <label className="text-sm font-medium text-zinc-300">
              Confirm password
            </label>
            <input
              value={confirmPassword}
              onChange={(event) => setConfirmPassword(event.target.value)}
              type="password"
              placeholder="Confirm password"
              className="input-shell"
              autoComplete="new-password"
            />
          </div>

          {error && (
            <div className="rounded-[20px] border border-red-500/20 bg-red-500/8 p-4 text-sm text-red-200">
              {error}
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            className="primary-button mt-2 w-full"
            data-pressable
            data-glow={!loading ? 'pulse' : undefined}
          >
            {loading ? 'Creating account...' : 'Create account'}
          </button>
        </form>
      }
      footer={
        <p>
          Already have an account?{' '}
          <Link to="/login" className="text-cyan-300" data-pressable>
            Log in
          </Link>
          .
        </p>
      }
    />
  )
}
