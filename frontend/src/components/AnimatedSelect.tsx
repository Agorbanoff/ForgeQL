import { useEffect, useId, useRef, useState } from 'react'
import { createPortal } from 'react-dom'

export type AnimatedSelectOption = {
  value: string
  label: string
  description?: string
}

type AnimatedSelectProps = {
  value: string
  options: readonly AnimatedSelectOption[]
  onChange: (value: string) => void
  placeholder?: string
  disabled?: boolean
  ariaLabel?: string
}

export function AnimatedSelect({
  value,
  options,
  onChange,
  placeholder = 'Select an option',
  disabled = false,
  ariaLabel,
}: AnimatedSelectProps) {
  const [open, setOpen] = useState(false)
  const rootRef = useRef<HTMLDivElement | null>(null)
  const triggerRef = useRef<HTMLButtonElement | null>(null)
  const menuRef = useRef<HTMLDivElement | null>(null)
  const listboxId = useId()
  const selectedOption = options.find((option) => option.value === value)
  const isOpen = !disabled && open
  const [menuStyle, setMenuStyle] = useState<{
    top: number
    left: number
    width: number
  } | null>(null)

  useEffect(() => {
    if (!isOpen) {
      return
    }

    function updateMenuPosition() {
      if (!triggerRef.current) {
        return
      }

      const rect = triggerRef.current.getBoundingClientRect()
      setMenuStyle({
        top: rect.bottom + 12,
        left: rect.left,
        width: rect.width,
      })
    }

    updateMenuPosition()

    function handlePointerDown(event: PointerEvent) {
      const target = event.target as Node
      if (
        rootRef.current?.contains(target) ||
        menuRef.current?.contains(target)
      ) {
        return
      }

      setOpen(false)
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setOpen(false)
      }
    }

    function handleViewportChange() {
      updateMenuPosition()
    }

    window.addEventListener('pointerdown', handlePointerDown)
    window.addEventListener('keydown', handleKeyDown)
    window.addEventListener('resize', handleViewportChange)
    window.addEventListener('scroll', handleViewportChange, true)

    return () => {
      window.removeEventListener('pointerdown', handlePointerDown)
      window.removeEventListener('keydown', handleKeyDown)
      window.removeEventListener('resize', handleViewportChange)
      window.removeEventListener('scroll', handleViewportChange, true)
    }
  }, [isOpen])

  return (
    <div
      ref={rootRef}
      className="select-shell"
      data-open={isOpen ? 'true' : 'false'}
    >
      <button
        ref={triggerRef}
        type="button"
        className="input-shell select-trigger"
        onClick={() => {
          if (!disabled) {
            setOpen((current) => !current)
          }
        }}
        aria-label={ariaLabel}
        aria-controls={listboxId}
        aria-expanded={isOpen}
        aria-haspopup="listbox"
        disabled={disabled}
        data-pressable
      >
        <span className={selectedOption ? 'text-zinc-100' : 'text-zinc-500'}>
          {selectedOption?.label ?? placeholder}
        </span>
        <span className="select-caret" aria-hidden="true" />
      </button>

      {isOpen && menuStyle
        ? createPortal(
            <div
              ref={menuRef}
              className="select-menu is-portal"
              id={listboxId}
              role="listbox"
              style={{
                position: 'fixed',
                top: `${menuStyle.top}px`,
                left: `${menuStyle.left}px`,
                width: `${menuStyle.width}px`,
              }}
            >
              {options.map((option) => {
                const active = option.value === value

                return (
                  <button
                    key={option.value}
                    type="button"
                    role="option"
                    aria-selected={active}
                    className={`select-option ${active ? 'is-active' : ''}`}
                    onClick={() => {
                      onChange(option.value)
                      setOpen(false)
                    }}
                    data-pressable
                  >
                    <span className="select-option-label">{option.label}</span>
                    {option.description && (
                      <span className="select-option-description">
                        {option.description}
                      </span>
                    )}
                  </button>
                )
              })}
            </div>,
            document.body
          )
        : null}
    </div>
  )
}
