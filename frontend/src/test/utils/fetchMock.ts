import { vi } from 'vitest'

type MockMatcher = string | RegExp | ((request: RecordedRequest) => boolean)

type MockHandler =
  | Response
  | ((request: RecordedRequest) => Response | Promise<Response>)

type MockRoute = {
  method: string
  matcher: MockMatcher
  handler: MockHandler
}

export type RecordedRequest = {
  method: string
  url: URL
  path: string
  bodyText: string
  bodyJson: unknown
  headers: Headers
}

function matchesRoute(matcher: MockMatcher, request: RecordedRequest) {
  if (typeof matcher === 'function') {
    return matcher(request)
  }

  if (matcher instanceof RegExp) {
    return matcher.test(request.path)
  }

  if (matcher.includes('?')) {
    return request.path === matcher
  }

  return request.url.pathname === matcher
}

async function toRecordedRequest(
  input: Parameters<typeof fetch>[0],
  init?: Parameters<typeof fetch>[1]
): Promise<RecordedRequest> {
  const rawUrl =
    typeof input === 'string'
      ? input
      : input instanceof URL
        ? input.toString()
        : input.url

  const url = new URL(rawUrl, 'http://localhost')
  const method = (
    init?.method ??
    (!(typeof input === 'string' || input instanceof URL) ? input.method : undefined) ??
    'GET'
  ).toUpperCase()
  const headers = new Headers(
    init?.headers ??
      (!(typeof input === 'string' || input instanceof URL) ? input.headers : undefined)
  )

  let bodyText = ''

  if (!(typeof input === 'string' || input instanceof URL)) {
    bodyText = await input.clone().text()
  } else if (typeof init?.body === 'string') {
    bodyText = init.body
  } else if (init?.body instanceof URLSearchParams) {
    bodyText = init.body.toString()
  } else if (init?.body != null) {
    bodyText = String(init.body)
  }

  let bodyJson: unknown = undefined

  if (bodyText) {
    try {
      bodyJson = JSON.parse(bodyText)
    } catch {
      bodyJson = undefined
    }
  }

  return {
    method,
    url,
    path: `${url.pathname}${url.search}`,
    bodyText,
    bodyJson,
    headers,
  }
}

export function createJsonResponse(body: unknown, init: ResponseInit = {}) {
  return new Response(JSON.stringify(body), {
    status: init.status ?? 200,
    headers: {
      'Content-Type': 'application/json',
      ...(init.headers ?? {}),
    },
  })
}

export function createApiErrorResponse(
  status: number,
  code: string,
  message: string,
  targetPath: string
) {
  return createJsonResponse(
    {
      timestamp: '2026-04-13T12:00:00.000Z',
      status,
      error: status >= 500 ? 'Internal Server Error' : 'Bad Request',
      code,
      message,
      targetPath,
    },
    { status }
  )
}

export function installFetchMock() {
  const routes: MockRoute[] = []
  const calls: RecordedRequest[] = []

  const fetchSpy = vi
    .spyOn(globalThis, 'fetch')
    .mockImplementation(async (input, init) => {
      const request = await toRecordedRequest(input, init)
      calls.push(request)

      const route = routes.find(
        (candidate) =>
          candidate.method === request.method &&
          matchesRoute(candidate.matcher, request)
      )

      if (!route) {
        throw new Error(`No fetch mock matched ${request.method} ${request.path}`)
      }

      if (route.handler instanceof Response) {
        return route.handler.clone()
      }

      return route.handler(request)
    })

  return {
    calls,
    route(method: string, matcher: MockMatcher, handler: MockHandler) {
      routes.push({
        method: method.toUpperCase(),
        matcher,
        handler,
      })
    },
    getCalls(method: string, matcher?: MockMatcher) {
      return calls.filter(
        (request) =>
          request.method === method.toUpperCase() &&
          (matcher ? matchesRoute(matcher, request) : true)
      )
    },
    fetchSpy,
  }
}
