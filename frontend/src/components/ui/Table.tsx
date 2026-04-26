import type { ReactNode } from 'react'

type Column<T> = {
  key: string
  header: ReactNode
  className?: string
  render: (item: T) => ReactNode
}

type Props<T> = {
  columns: Array<Column<T>>
  rows: T[]
  getRowKey: (item: T) => string | number
  emptyState: ReactNode
}

export function Table<T>({
  columns,
  rows,
  getRowKey,
  emptyState,
}: Props<T>) {
  return (
    <div className="overflow-hidden rounded-[24px] border border-white/8">
      <div className="max-h-[28rem] overflow-auto">
        <table className="data-table min-w-full">
          <thead>
            <tr>
              {columns.map((column) => (
                <th key={column.key} className={column.className}>
                  {column.header}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {rows.length > 0 ? (
              rows.map((row) => (
                <tr key={getRowKey(row)}>
                  {columns.map((column) => (
                    <td key={column.key} className={column.className}>
                      {column.render(row)}
                    </td>
                  ))}
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan={columns.length}>{emptyState}</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
