type QueryPayload = {
  entity?: string
  fields?: string[]
  filter?: Record<string, Record<string, unknown>>
  limit?: number
}

const DEMO_DATA = {
  users: [
    { id: 1, username: 'Aura', email: 'aura@house.dev', age: 24 },
    { id: 2, username: 'Kolev', email: 'kolev@house.dev', age: 27 },
    { id: 3, username: 'Mira', email: 'mira@house.dev', age: 31 },
    { id: 4, username: 'Dogan', email: 'dogan@house.dev', age: 29 },
  ],
  posts: [
    { id: 101, user_id: 1, title: 'Welcome Home', created_at: '2026-03-10', likes: 18 },
    { id: 102, user_id: 2, title: 'Memory Lane', created_at: '2026-03-12', likes: 42 },
    { id: 103, user_id: 2, title: 'Summer Room Tour', created_at: '2026-03-16', likes: 26 },
    { id: 104, user_id: 4, title: 'House Of Memories Launch', created_at: '2026-03-20', likes: 54 },
  ],
} as const

export async function runQuery(payload: unknown) {
  await wait(350)

  const query = (payload ?? {}) as QueryPayload
  const entityName = query.entity === 'posts' ? 'posts' : 'users'
  const rows = [...DEMO_DATA[entityName]]

  const filteredRows = applyFilters(rows, query.filter)
  const limitedRows =
    typeof query.limit === 'number' && query.limit > 0
      ? filteredRows.slice(0, query.limit)
      : filteredRows

  return limitedRows.map((row) => projectFields(row, query.fields))
}

function applyFilters<T extends Record<string, unknown>>(
  rows: T[],
  filters?: Record<string, Record<string, unknown>>
) {
  if (!filters || Object.keys(filters).length === 0) {
    return rows
  }

  return rows.filter((row) =>
    Object.entries(filters).every(([field, operations]) =>
      Object.entries(operations).every(([operator, expected]) =>
        matches(row[field], operator, expected)
      )
    )
  )
}

function matches(actual: unknown, operator: string, expected: unknown) {
  switch (operator) {
    case 'eq':
      return actual === expected
    case 'ne':
      return actual !== expected
    case 'gt':
      return compare(actual, expected) > 0
    case 'gte':
      return compare(actual, expected) >= 0
    case 'lt':
      return compare(actual, expected) < 0
    case 'lte':
      return compare(actual, expected) <= 0
    case 'like':
      return String(actual ?? '')
        .toLowerCase()
        .includes(String(expected ?? '').replaceAll('%', '').toLowerCase())
    default:
      return true
  }
}

function compare(actual: unknown, expected: unknown) {
  if (typeof actual === 'number' && typeof expected === 'number') {
    return actual - expected
  }

  return String(actual ?? '').localeCompare(String(expected ?? ''))
}

function projectFields<T extends Record<string, unknown>>(
  row: T,
  fields?: string[]
) {
  if (!fields || fields.length === 0) {
    return row
  }

  return fields.reduce<Record<string, unknown>>((acc, field) => {
    acc[field] = row[field]
    return acc
  }, {})
}

function wait(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}
