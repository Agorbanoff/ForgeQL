import type { DatasourceAccessRole, GlobalRole } from '../../types/platform'

type Role = GlobalRole | DatasourceAccessRole

function getTone(role: Role) {
  if (role === 'MAIN_ADMIN') {
    return 'border-fuchsia-300/25 bg-fuchsia-300/10 text-fuchsia-100'
  }

  if (role === 'ADMIN' || role === 'MANAGER') {
    return 'border-cyan-300/25 bg-cyan-300/10 text-cyan-100'
  }

  if (role === 'MEMBER') {
    return 'border-amber-300/25 bg-amber-300/10 text-amber-100'
  }

  return 'border-white/10 bg-white/[0.04] text-zinc-200'
}

export function RoleBadge({ role }: { role: Role }) {
  return (
    <span
      className={`inline-flex items-center rounded-full border px-3 py-1 text-xs font-semibold tracking-[0.16em] ${getTone(
        role
      )}`}
    >
      {role}
    </span>
  )
}
