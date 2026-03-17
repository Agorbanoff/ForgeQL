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
  return (
    <div className="space-y-4">
      <h2 className="text-lg font-semibold">Response</h2>

      {loading && (
        <div className="rounded-xl border border-zinc-800 bg-zinc-950 p-4 text-sm text-zinc-300">
          Loading...
        </div>
      )}

      {error && (
        <div className="rounded-xl border border-red-800 bg-red-950/40 p-4 text-sm text-red-300">
          {error}
        </div>
      )}

      {!loading && !error && data === null && (
        <div className="rounded-xl border border-zinc-800 bg-zinc-950 p-4 text-sm text-zinc-500">
          No response yet.
        </div>
      )}

      {!loading && !error && data !== null && (
        <pre className="overflow-auto rounded-xl border border-zinc-800 bg-black p-4 text-xs text-green-400">
          {JSON.stringify(data, null, 2)}
        </pre>
      )}
    </div>
  )
}