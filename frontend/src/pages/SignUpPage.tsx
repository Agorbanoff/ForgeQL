import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { signUpUser } from '../api/accountApi'
import { clearSavedDatasource, clearSessionActive } from '../lib/appState'
import AuthLayout from '../components/AuthLayout'

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
      setError('Email is required.')
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

      clearSessionActive()
      clearSavedDatasource()
      setSuccess('Account created successfully. Redirecting to log in...')
      setUsername('')
      setEmail('')
      setPassword('')
      setConfirmPassword('')

      setTimeout(() => {
        navigate('/login')
      }, 800)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Sign up failed.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthLayout
      badge="Smooth Onboarding"
      title="Design a cleaner path into your data layer."
      description="Create a SigmaQL account and move directly into datasource setup with the same polished motion language, dark surfaces, and editorial feel carried across the whole product."
      formTitle="Create account"
      formDescription="Create your account with a real email and password, then log in to start a session."
      form={
        <form className="space-y-4" onSubmit={handleSubmit}>
          <div className="space-y-2">
            <label className="text-sm font-medium text-zinc-300">Username</label>
            <input
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="Username"
              className="input-shell"
            />
          </div>

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

          <div className="space-y-2">
            <label className="text-sm font-medium text-zinc-300">
              Confirm password
            </label>
            <input
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              type="password"
              placeholder="Confirm password"
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
