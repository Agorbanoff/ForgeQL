import type { ReactNode } from 'react'
import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import ConnectionRequestPage from './pages/ConnectionRequestPage'
import LandingPage from './pages/LandingPage'
import LoginPage from './pages/LoginPage'
import PlaygroundPage from './pages/PlaygroundPage'
import SignUpPage from './pages/SignUpPage'
import { hasLocalSession, hasSavedDatasource } from './lib/appState'
import { useElegantAnimations } from './hooks/useElegantAnimations'

function getNextPath() {
  if (!hasLocalSession()) {
    return '/login'
  }

  return hasSavedDatasource() ? '/playground' : '/connection-request'
}

function LandingOrRedirect() {
  if (hasLocalSession()) {
    return <Navigate to={getNextPath()} replace />
  }

  return <LandingPage />
}

function RequireLoggedOut({ children }: { children: ReactNode }) {
  if (hasLocalSession()) {
    return <Navigate to={getNextPath()} replace />
  }

  return <>{children}</>
}

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

export default function App() {
  const location = useLocation()
  const shellRef = useElegantAnimations<HTMLDivElement>([location.pathname])

  return (
    <div ref={shellRef} className="relative min-h-screen overflow-x-hidden">
      <div className="pointer-events-none absolute inset-0 overflow-hidden">
        <div
          className="absolute left-[-10rem] top-[-4rem] h-[28rem] w-[28rem] rounded-full bg-[radial-gradient(circle,_rgba(255,222,115,0.26),_transparent_68%)] blur-3xl"
          data-float="slow"
          data-glow="pulse"
        />
        <div
          className="absolute right-[-6rem] top-[-4rem] h-[24rem] w-[24rem] rounded-full bg-[radial-gradient(circle,_rgba(255,190,207,0.18),_transparent_70%)] blur-3xl"
          data-float="medium"
        />
        <div
          className="absolute bottom-[-10rem] left-[12%] h-[26rem] w-[26rem] rounded-full bg-[radial-gradient(circle,_rgba(82,215,255,0.22),_transparent_70%)] blur-3xl"
          data-float="slow"
        />
      </div>

      <div key={location.pathname} data-animate="scene">
        <Routes location={location}>
          <Route path="/" element={<LandingOrRedirect />} />
          <Route
            path="/login"
            element={
              <RequireLoggedOut>
                <LoginPage />
              </RequireLoggedOut>
            }
          />
          <Route
            path="/signup"
            element={
              <RequireLoggedOut>
                <SignUpPage />
              </RequireLoggedOut>
            }
          />
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
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </div>
    </div>
  )
}
