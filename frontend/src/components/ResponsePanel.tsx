import { useElegantAnimations } from '../hooks/useElegantAnimations'

type ResponsePanelProps = {
  data: unknown
  error: string | null
  loading: boolean
}

export function ResponsePanel({
  data,
  error,
  loading,
}: ResponsePanelProps) {
  const rootRef = useElegantAnimations<HTMLDivElement>([
    loading,
    Boolean(error),
    data === null,
  ])
  const statusLabel = loading ? 'Running' : error ? 'Error' : data === null ? 'Idle' : 'Complete'

  return (
    <div ref={rootRef} className="space-y-4">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">
            Response
          </p>
          <h2 className="display-title mt-3 text-[2rem] text-white">Output</h2>
        </div>
        <span className="small-chip">{statusLabel}</span>
      </div>

      <div className="surface-card p-5" data-animate="panel" data-tilt>
        {loading && (
          <div className="rounded-[22px] border border-white/8 bg-white/[0.02] p-5 text-sm text-zinc-200">
            <div className="flex items-center gap-3">
              <span className="h-2.5 w-2.5 rounded-full bg-cyan-300" data-glow="pulse" />
              Running query...
            </div>
          </div>
        )}

        {error && (
          <div className="rounded-[22px] border border-red-500/20 bg-red-500/8 p-5 text-sm text-red-200">
            {error}
          </div>
        )}

        {!loading && !error && data === null && (
          <div className="rounded-[22px] border border-white/8 bg-white/[0.02] p-5 text-sm text-zinc-500">
            No response yet. Run a query to populate this panel.
          </div>
        )}

        {!loading && !error && data !== null && (
          <pre className="data-block">{JSON.stringify(data, null, 2)}</pre>
        )}
      </div>
    </div>
  )
}
