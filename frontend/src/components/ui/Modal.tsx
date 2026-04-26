import { useEffect, type ReactNode } from 'react'
import { createPortal } from 'react-dom'

type Props = {
  open: boolean
  title: string
  description?: string
  onClose: () => void
  children: ReactNode
}

export function Modal({ open, title, description, onClose, children }: Props) {
  useEffect(() => {
    if (!open) {
      return
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        onClose()
      }
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [onClose, open])

  if (!open) {
    return null
  }

  return createPortal(
    <div className="fixed inset-0 z-[140] flex items-center justify-center bg-slate-950/80 px-4 backdrop-blur-md">
      <div className="surface-panel w-full max-w-5xl px-6 py-6 sm:px-8">
        <div className="relative z-10">
          <div className="flex items-start justify-between gap-4">
            <div>
              <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">
                Manage access
              </p>
              <h2 className="mt-3 text-3xl font-semibold text-white">{title}</h2>
              {description ? (
                <p className="mt-3 max-w-2xl text-sm leading-6 text-zinc-300">
                  {description}
                </p>
              ) : null}
            </div>
            <button
              type="button"
              className="secondary-button h-11 w-11 rounded-full p-0 text-xl"
              onClick={onClose}
              aria-label="Close modal"
            >
              ×
            </button>
          </div>
          <div className="mt-6">{children}</div>
        </div>
      </div>
    </div>,
    document.body
  )
}
