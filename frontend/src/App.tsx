import type { ReactNode } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import PlaygroundPage from './pages/PlaygroundPage'
import LoginPage from './pages/LoginPage'
import SignUpPage from './pages/SignUpPage'
import ConnectionRequestPage from './pages/ConnectionRequestPage'
import { hasLocalSession, hasSavedDatasource } from './lib/appState'

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
  return (
    <div className="min-h-screen bg-zinc-950 text-white">
      <header className="border-b border-zinc-800">
        <div className="mx-auto flex max-w-7xl items-center px-6 py-4">
          <h1 className="text-xl font-semibold">ForgeQL</h1>
        </div>
      </header>

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
