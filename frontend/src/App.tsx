import { Link, Route, Routes, useLocation } from 'react-router-dom'
import PlaygroundPage from './pages/PlaygroundPage'
import LoginPage from './pages/LoginPage'
import SignUpPage from './pages/SignUpPage'
import ConnectionRequestPage from './pages/ConnectionRequestPage'

export default function App() {
  const location = useLocation()

  const navItems = [
    { to: '/', label: 'Playground' },
    { to: '/login', label: 'Log in' },
    { to: '/signup', label: 'Sign up' },
    { to: '/connection-request', label: 'DB Access' },
  ]

  return (
    <div className="min-h-screen bg-zinc-950 text-white">
      <header className="border-b border-zinc-800">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-6 py-4">
          <h1 className="text-xl font-semibold">ForgeQL</h1>

          <nav className="flex flex-wrap gap-2">
            {navItems.map((item) => {
              const active = location.pathname === item.to

              return (
                <Link
                  key={item.to}
                  to={item.to}
                  className={`rounded-lg px-4 py-2 text-sm ${
                    active
                      ? 'bg-cyan-500 text-black'
                      : 'border border-zinc-700 text-zinc-300'
                  }`}
                >
                  {item.label}
                </Link>
              )
            })}
          </nav>
        </div>
      </header>

      <Routes>
        <Route path="/" element={<PlaygroundPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignUpPage />} />
        <Route path="/connection-request" element={<ConnectionRequestPage />} />
      </Routes>
    </div>
  )
}