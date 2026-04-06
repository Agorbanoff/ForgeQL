import { useEffect, useId, useRef, useState } from 'react'

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
  const listboxId = useId()
  const selectedOption = options.find((option) => option.value === value)
  const isOpen = !disabled && open

  useEffect(() => {
    if (!isOpen) {
      return
    }

    function handlePointerDown(event: PointerEvent) {
      if (rootRef.current?.contains(event.target as Node)) {
        return
      }

      setOpen(false)
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setOpen(false)
      }
    }

    window.addEventListener('pointerdown', handlePointerDown)
    window.addEventListener('keydown', handleKeyDown)

    return () => {
      window.removeEventListener('pointerdown', handlePointerDown)
      window.removeEventListener('keydown', handleKeyDown)
    }
  }, [isOpen])

  return (
    <div
      ref={rootRef}
      className="select-shell"
      data-open={isOpen ? 'true' : 'false'}
    >
      <button
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

      <div className="select-menu" id={listboxId} role="listbox">
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
      </div>
    </div>
  )
}
