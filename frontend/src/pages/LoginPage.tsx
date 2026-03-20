import { Link } from 'react-router-dom'

export default function LoginPage() {
  return (
    <main className="flex min-h-[calc(100vh-80px)] items-center justify-center">
      <div className="w-full max-w-md rounded-xl border border-zinc-800 bg-zinc-900 p-6">
        <h2 className="mb-4 text-2xl font-semibold">Log in</h2>

        <div className="space-y-4">
          <input
            placeholder="Email"
            className="w-full rounded-lg border border-zinc-700 bg-zinc-800 p-3"
          />
          <input
            type="password"
            placeholder="Password"
            className="w-full rounded-lg border border-zinc-700 bg-zinc-800 p-3"
          />
          <button className="w-full rounded-lg bg-cyan-500 p-3 text-black">
            Log in
          </button>
        </div>

        <p className="mt-4 text-sm text-zinc-400">
          No account?{' '}
          <Link to="/signup" className="text-cyan-400">
            Sign up
          </Link>
        </p>
      </div>
    </main>
  )
}