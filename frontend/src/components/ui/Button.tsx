import type { ButtonHTMLAttributes, ReactNode } from 'react'

type ButtonVariant = 'primary' | 'secondary' | 'danger' | 'ghost'

type Props = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant
  children: ReactNode
}

const variantClassNames: Record<ButtonVariant, string> = {
  primary: 'primary-button',
  secondary: 'secondary-button',
  danger:
    'inline-flex items-center justify-center gap-2 rounded-full border border-red-400/20 bg-red-500/10 px-5 py-3 text-sm font-semibold text-red-100 transition hover:bg-red-500/15 disabled:cursor-not-allowed disabled:opacity-60',
  ghost:
    'inline-flex items-center justify-center gap-2 rounded-full px-4 py-2 text-sm font-semibold text-zinc-300 transition hover:bg-white/5 hover:text-white disabled:cursor-not-allowed disabled:opacity-60',
}

export function Button({
  variant = 'secondary',
  className = '',
  children,
  ...props
}: Props) {
  return (
    <button
      type="button"
      className={`${variantClassNames[variant]} ${className}`.trim()}
      {...props}
    >
      {children}
    </button>
  )
}
