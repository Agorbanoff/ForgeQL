import type { ReactNode } from 'react'
import { Link, Navigate, Route, Routes, useLocation } from 'react-router-dom'
import PlaygroundPage from './pages/PlaygroundPage'
import LoginPage from './pages/LoginPage'
import SignUpPage from './pages/SignUpPage'
import ConnectionRequestPage from './pages/ConnectionRequestPage'
import { hasLocalSession, hasSavedDatasource } from './lib/appState'
import { useElegantAnimations } from './hooks/useElegantAnimations'

function RequireSession({ children }: { children: ReactNode }) {
  if (!hasLocalSession()) {
    return <Navigate to="/login" replace />
  }

  return <>{children}</>
}

function RequireSavedDatasource({ children }: { children: ReactNode }) {
  if (!hasSavedDatasource()) {
    return <Navigate to="/connection-request" replace />
  }

  return <>{children}</>
}

const ROUTE_META: Record<string, { label: string; detail: string }> = {
  '/login': {
    label: 'Client Login',
    detail: 'Elegant access into the SigmaQL workspace',
  },
  '/signup': {
    label: 'Create Account',
    detail: 'Set up your query surface in a few clean steps',
  },
  '/connection-request': {
    label: 'Connection Intake',
    detail: 'Configure a datasource with a calmer onboarding flow',
  },
  '/playground': {
    label: 'Playground',
    detail: 'Explore the schema and run expressive JSON queries',
  },
}

const NAV_ITEMS = [
  { to: '/login', label: 'Log in' },
  { to: '/signup', label: 'Sign up' },
  { to: '/connection-request', label: 'Connect DB' },
  { to: '/playground', label: 'Playground' },
]

export default function App() {
  const location = useLocation()
  const shellRef = useElegantAnimations<HTMLDivElement>([location.pathname])
  const sessionActive = hasLocalSession()
  const workspaceReady = hasSavedDatasource()
  const routeMeta = ROUTE_META[location.pathname] ?? ROUTE_META['/login']
  const ctaHref = workspaceReady
    ? '/playground'
    : sessionActive
      ? '/connection-request'
      : '/signup'
  const ctaLabel = workspaceReady
    ? 'Open workspace'
    : sessionActive
      ? 'Connect datasource'
      : 'Get started'

  return (
    <div ref={shellRef} className="relative min-h-screen overflow-hidden">
      <div className="pointer-events-none absolute inset-0 overflow-hidden">
        <div
          className="absolute left-[-10rem] top-[-4rem] h-[28rem] w-[28rem] rounded-full bg-[radial-gradient(circle,_rgba(255,222,115,0.34),_transparent_68%)] blur-3xl"
          data-float="slow"
          data-glow="pulse"
        />
        <div
          className="absolute right-[-6rem] top-[-4rem] h-[24rem] w-[24rem] rounded-full bg-[radial-gradient(circle,_rgba(255,190,207,0.28),_transparent_70%)] blur-3xl"
          data-float="medium"
        />
        <div
          className="absolute bottom-[-10rem] left-[12%] h-[26rem] w-[26rem] rounded-full bg-[radial-gradient(circle,_rgba(82,215,255,0.22),_transparent_70%)] blur-3xl"
          data-float="slow"
        />
      </div>

      <div className="page-shell pt-4 sm:pt-5">
        <header className="sticky top-4 z-40" data-animate="hero">
          <div className="surface-panel rounded-full px-4 py-3 sm:px-6">
            <div className="relative z-10 flex flex-wrap items-center gap-4">
              <Link to="/login" className="flex items-center gap-3" data-pressable>
                <div className="flex h-11 w-11 items-center justify-center rounded-full border border-cyan-400/25 bg-cyan-500/10 text-cyan-200">
                  <span className="display-title text-3xl leading-none">*</span>
                </div>

                <div>
                  <p className="text-xs uppercase tracking-[0.28em] text-zinc-500">
                    SigmaQL
                  </p>
                  <p className="text-sm font-medium text-zinc-100">
                    Schema-first query design
                  </p>
                </div>
              </Link>

              <nav className="hidden items-center gap-2 rounded-full border border-white/8 bg-white/[0.03] p-1 md:flex">
                {NAV_ITEMS.map((item) => {
                  const active = location.pathname === item.to

                  return (
                    <Link
                      key={item.to}
                      to={item.to}
                      className={`rounded-full px-4 py-2 text-sm transition ${
                        active
                          ? 'bg-white text-zinc-950'
                          : 'text-zinc-400 hover:text-zinc-100'
                      }`}
                      data-pressable
                    >
                      {item.label}
                    </Link>
                  )
                })}
              </nav>

              <div className="ml-auto hidden text-right lg:block">
                <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">
                  {routeMeta.label}
                </p>
                <p className="mt-1 text-sm text-zinc-300">{routeMeta.detail}</p>
              </div>

              <Link to={ctaHref} className="primary-button ml-auto md:ml-0" data-pressable>
                {ctaLabel}
              </Link>
            </div>
          </div>
        </header>
      </div>

      <Routes>
        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignUpPage />} />
        <Route
          path="/connection-request"
          element={
            <RequireSession>
              <ConnectionRequestPage />
            </RequireSession>
          }
        />
        <Route
          path="/playground"
          element={
            <RequireSession>
              <RequireSavedDatasource>
                <PlaygroundPage />
              </RequireSavedDatasource>
            </RequireSession>
          }
        />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </div>
  )
}
