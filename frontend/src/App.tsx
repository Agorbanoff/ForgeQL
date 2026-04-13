import type { ReactNode } from 'react'
import { Navigate, Route, Routes, useParams } from 'react-router-dom'
import { useAuth } from './auth/AuthProvider'
import ConnectionRequestPage from './pages/ConnectionRequestPage'
import LandingPage from './pages/LandingPage'
import LoginPage from './pages/LoginPage'
import PlaygroundPage from './pages/PlaygroundPage'
import SignUpPage from './pages/SignUpPage'
import { getStoredDatasourceDetails } from './lib/appState'

function AppLoadingScreen() {
  return (
    <main className="page-shell flex min-h-screen items-center py-10 sm:py-14">
      <section className="surface-panel mx-auto w-full max-w-3xl px-6 py-8 text-center sm:px-8 sm:py-10">
        <div className="relative z-10">
          <span className="section-badge">Session bootstrap</span>
          <h1 className="display-title mt-6 text-[3rem] text-white sm:text-[3.8rem]">
            Checking your workspace session.
          </h1>
          <p className="display-copy mt-4 text-sm sm:text-base">
            ForgeQL is verifying the backend cookies before routing into the
            datasource workspace.
          </p>
        </div>
      </section>
    </main>
  )
}

function getLegacyExplorerPath() {
  const selection = getStoredDatasourceDetails()
  return selection ? `/datasource/${selection.id}/explorer` : '/datasource'
}

function LandingOrRedirect() {
  const { status } = useAuth()

  if (status === 'loading') {
    return <AppLoadingScreen />
  }

  if (status === 'authenticated') {
    return <Navigate to="/datasource" replace />
  }

  return <LandingPage />
}

function RequireLoggedOut({ children }: { children: ReactNode }) {
  const { status } = useAuth()

  if (status === 'loading') {
    return <AppLoadingScreen />
  }

  if (status === 'authenticated') {
    return <Navigate to="/datasource" replace />
  }

  return <>{children}</>
}

function RequireSession({ children }: { children: ReactNode }) {
  const { status } = useAuth()

  if (status === 'loading') {
    return <AppLoadingScreen />
  }

  if (status !== 'authenticated') {
    return <Navigate to="/login" replace />
  }

  return <>{children}</>
}

function LegacyPlaygroundRedirect() {
  return <Navigate to={getLegacyExplorerPath()} replace />
}

function DatasourceExplorerRedirect() {
  const { datasourceId } = useParams()
  return <Navigate to={`/datasource/${datasourceId}/explorer`} replace />
}

export default function App() {
  return (
    <div className="relative min-h-screen overflow-x-hidden">
      <div className="pointer-events-none absolute inset-0 overflow-hidden">
        <div className="absolute left-[-10rem] top-[-4rem] h-[28rem] w-[28rem] rounded-full bg-[radial-gradient(circle,_rgba(255,222,115,0.26),_transparent_68%)] blur-3xl" />
        <div className="absolute right-[-6rem] top-[-4rem] h-[24rem] w-[24rem] rounded-full bg-[radial-gradient(circle,_rgba(255,190,207,0.18),_transparent_70%)] blur-3xl" />
        <div className="absolute bottom-[-10rem] left-[12%] h-[26rem] w-[26rem] rounded-full bg-[radial-gradient(circle,_rgba(82,215,255,0.22),_transparent_70%)] blur-3xl" />
      </div>

      <Routes>
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
          path="/datasource"
          element={
            <RequireSession>
              <ConnectionRequestPage />
            </RequireSession>
          }
        />
        <Route
          path="/datasource/:datasourceId"
          element={
            <RequireSession>
              <DatasourceExplorerRedirect />
            </RequireSession>
          }
        />
        <Route
          path="/datasource/:datasourceId/explorer"
          element={
            <RequireSession>
              <PlaygroundPage />
            </RequireSession>
          }
        />
        <Route path="/connection-request" element={<Navigate to="/datasource" replace />} />
        <Route path="/playground" element={<LegacyPlaygroundRedirect />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </div>
  )
}
