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

  return (
    <div ref={rootRef} className="space-y-5">
      <div>
        <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">
          Response view
        </p>
        <h2 className="display-title mt-3 text-[2.2rem] text-white">Inspect output</h2>
        <p className="mt-3 text-sm leading-6 text-zinc-400">
          Review the result state instantly, from loading and errors to the final JSON payload.
        </p>
      </div>

      <div className="surface-card p-5" data-animate="panel">
        <div className="flex items-center justify-between gap-3">
          <p className="text-xs uppercase tracking-[0.2em] text-zinc-500">Status</p>
          <span className="rounded-full border border-white/8 bg-white/[0.03] px-3 py-1.5 text-xs text-zinc-300">
            {loading ? 'Running' : error ? 'Error' : data === null ? 'Idle' : 'Complete'}
          </span>
        </div>

        {loading && (
          <div className="mt-5 rounded-[22px] border border-white/8 bg-white/[0.02] p-5 text-sm text-zinc-200">
            <div className="flex items-center gap-3">
              <span className="h-2.5 w-2.5 rounded-full bg-cyan-300" data-glow="pulse" />
              Generating response preview...
            </div>
          </div>
        )}

        {error && (
          <div className="mt-5 rounded-[22px] border border-red-500/20 bg-red-500/8 p-5 text-sm text-red-200">
            {error}
          </div>
        )}

        {!loading && !error && data === null && (
          <div className="mt-5 rounded-[22px] border border-white/8 bg-white/[0.02] p-5 text-sm text-zinc-500">
            No response yet. Run a query from the builder to populate this panel.
          </div>
        )}

        {!loading && !error && data !== null && (
          <pre className="data-block mt-5">{JSON.stringify(data, null, 2)}</pre>
        )}
      </div>
    </div>
  )
}
