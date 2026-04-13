import { render } from '@testing-library/react'
import type { ReactElement } from 'react'
import { MemoryRouter } from 'react-router-dom'
import App from '../../App'
import { AuthProvider } from '../../auth/AuthProvider'

export function renderWithRouter(ui: ReactElement, route = '/') {
  return render(<MemoryRouter initialEntries={[route]}>{ui}</MemoryRouter>)
}

export function renderAppAt(route: string) {
  return renderWithRouter(
    <AuthProvider>
      <App />
    </AuthProvider>,
    route
  )
}
