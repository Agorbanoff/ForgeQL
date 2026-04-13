# ForgeQL

ForgeQL is a PostgreSQL-focused dynamic backend engine for connecting to user-managed databases at runtime, generating a schema snapshot from live metadata, and exposing safe read, aggregate, and mutation operations without hardcoded entity classes per target database. It is designed for the case where the application does not own the target schema ahead of time, but still needs a disciplined execution model: datasource resolution, PostgreSQL validation, schema introspection, AST construction, plan validation, SQL generation, and JDBC execution.

This `backend` module contains the execution engine and the application state store for users, saved datasources, and refresh tokens. A sibling React frontend exists in `../frontend` for authentication, datasource onboarding, and presentation flow. Some older files still carry the earlier `SigmaQL` name, but the runtime architecture in this codebase is the current ForgeQL implementation.

## Key Features

- PostgreSQL-only datasource management with encrypted stored passwords, ownership checks, and runtime connection testing.
- Runtime PostgreSQL connection resolution with SSL modes, timeouts, optional application name, root certificate support, and extra JDBC options.
- Dynamic schema generation from live PostgreSQL metadata, including tables, views, materialized views, columns, primary keys, unique constraints, foreign keys, relations, enum labels, and schema fingerprints.
- Schema browsing endpoints for full schema snapshots, summaries, table discovery, column inspection, and relation inspection.
- Read queries with explicit projections, validated filters, sorting, pagination, and schema-qualified execution.
- Aggregate queries with `count`, `sum`, `avg`, `min`, and `max`, plus `groupBy` and validated filters.
- Safe mutations for insert, update, and delete on tables that have exactly one primary key column.
- Schema-driven value coercion for PostgreSQL-specific types such as enums, UUID, JSON/JSONB, arrays, numerics, and timestamp types.
- Stable API error responses with application error codes and PostgreSQL-aware failure classification.
- Lazy runtime connection pools keyed by datasource id and connection fingerprint, with bounded pool counts and idle eviction.

## Architecture Overview

ForgeQL is built around a schema-driven execution path:

```text
saved datasource
  -> runtime connection definition
  -> PostgreSQL connection + product validation
  -> metadata introspection
  -> generated schema snapshot
  -> request AST
  -> validated execution plan
  -> PostgreSQL SQL
  -> JDBC execution
  -> API response
```

The important design choice is that the generated schema becomes the source of truth for all dynamic operations. Table names, column names, relation edges, writable fields, aggregatable fields, and mutation capability are resolved from that snapshot rather than trusted from client input. That keeps the runtime flexible enough for arbitrary PostgreSQL schemas while still enforcing deterministic validation rules.

From a system split perspective:

- The Spring Boot backend owns authentication, datasource persistence, schema generation, planning, SQL generation, and execution.
- The React frontend provides the user-facing flow around login, datasource setup, and a presentation workspace.
- The backend execution engine is ahead of the current frontend integration: the `/core/datasources/...` API is implemented, while the current frontend playground still renders a local demo schema and mock query results.

## Supported Functionality

### Schema generation and browsing

- Generates a live schema snapshot per datasource through PostgreSQL introspection.
- Excludes PostgreSQL system schemas such as `pg_catalog`, `information_schema`, `pg_toast`, and temporary schemas.
- Discovers supported objects in user schemas:
  - tables
  - views
  - materialized views
- Models columns, nullability, defaults, identity/generated flags, precision, scale, length, enum labels, unique constraints, primary keys, foreign keys, and relation graph.
- Computes and stores a fingerprint for the generated snapshot.
- Keeps full schema snapshots in an in-memory registry keyed by datasource id.

### Reads, filters, and aggregates

- Table browsing:
  - full table list
  - per-table detail
  - columns
  - relations
- Row exploration:
  - explicit column projection
  - sort by validated fields
  - `limit` and `offset`
  - filters combined with top-level `AND`
- Supported read/aggregate filter operators:
  - `eq`
  - `ne`
  - `gt`
  - `gte`
  - `lt`
  - `lte`
  - `in`
  - `between`
  - `like`
  - `ilike`
  - `isNull`
  - `isNotNull`
- Aggregate functions:
  - `count`
  - `sum`
  - `avg`
  - `min`
  - `max`
- Aggregates support validated field selection, aliasing, optional `groupBy`, and the same validated filter language used by reads.

### Mutations and capability model

ForgeQL computes object capabilities from schema metadata and enforces them at planning time:

| Object type | Read | Aggregate | Insert | Update | Delete |
| --- | --- | --- | --- | --- | --- |
| `TABLE` with exactly one PK column | Yes | Yes | Yes | Yes | Yes |
| `TABLE` with no PK | Yes | Yes | No | No | No |
| `TABLE` with composite PK | Yes | Yes | No | No | No |
| `VIEW` | Yes | Yes | No | No | No |
| `MATERIALIZED_VIEW` | Yes | Yes | No | No | No |

Mutation behavior in the current implementation:

- Inserts, updates, and deletes are available only for real tables with exactly one primary key column.
- Update and delete routes are primary-key scoped only.
- Insert, update, and delete use `RETURNING` and return the affected row payload or identity.
- Generated columns, identity columns, and sequence-backed columns are not writable.
- Empty update payloads are rejected.
- Tables without a stable single-column identity remain read/aggregate only in v1.

### PostgreSQL-specific validation

Value coercion is schema-aware and currently covers:

- booleans
- `int2`, `int4`, `int8`
- `numeric`
- string-like types such as `text`, `varchar`, `bpchar`, `char`, `citext`, `name`
- UUID
- PostgreSQL enums
- JSON / JSONB
- arrays
- `timestamp`
- `timestamptz`

Unsupported mutation types are rejected explicitly rather than silently string-cast.

### Error handling and runtime policy

- Errors are returned through a shared `ApiErrorResponse` shape with HTTP status, stable application error code, safe message, datasource id when available, and request path.
- PostgreSQL failures are classified into categories such as timeout, authentication failure, SSL failure, unique constraint violation, and foreign key violation.
- Runtime pools are created lazily on first use, bounded by `core.postgres.pool.max-active-pools`, keyed by datasource id plus connection fingerprint, and evicted after inactivity.
- Schema snapshots are evicted when datasource connection details change or the datasource is deleted.

## Tech Stack

### Backend

- Java 21
- Spring Boot 3.5
- Spring Web
- Spring Data JPA
- Spring Security
- Spring Validation
- PostgreSQL JDBC driver
- HikariCP via Spring Boot datasource management
- JJWT for access and refresh token handling
- Maven
- Lombok

### Frontend

- React 19
- TypeScript
- Vite
- React Router
- Tailwind CSS v4
- Anime.js
- ESLint

### Infrastructure and runtime

- PostgreSQL 16 for the local application database through Docker Compose
- JDBC-based runtime connections to user-managed PostgreSQL datasources
- Environment-driven configuration through Spring properties and Vite environment variables

## Running the Project

### What needs to run

ForgeQL needs two different database concepts:

1. The application database used by this backend itself for users, saved datasources, and refresh tokens.
2. The target PostgreSQL datasources that users register and query through the engine.

The bundled Docker Compose file covers the first one only.

### Prerequisites

- Java 21
- Maven 3.9+
- Node.js 20+ for the frontend
- Docker Desktop or Docker Engine for the local PostgreSQL dependency

### 1. Start the application database with Docker

From the `backend` directory, run:

```bash
docker compose -f src/main/resources/docker-compose.yaml up -d
```

This starts PostgreSQL 16 on `localhost:5434` using the values in `src/main/resources/env.properties`.

### 2. Configure backend environment

The backend reads `src/main/resources/application.properties` and optionally imports `src/main/resources/env.properties`.

Important variables:

| Variable | Purpose | Required |
| --- | --- | --- |
| `APP_DATASOURCE_URL` | JDBC URL for ForgeQL's own application database | Yes |
| `APP_DATASOURCE_USERNAME` | Username for the application database | Yes |
| `APP_DATASOURCE_PASSWORD` | Password for the application database | Yes |
| `APP_DATASOURCE_DRIVER` | JDBC driver class, default `org.postgresql.Driver` | No |
| `DATASOURCE_ENCRYPTION_SECRET` | Secret used to encrypt saved target datasource passwords | Yes |
| `JWT_SECRET` | Signing key for access and refresh tokens | Yes |
| `SERVER_PORT` | Backend HTTP port, default `8080` | No |
| `CORE_POSTGRES_POOL_MAX_ACTIVE_POOLS` | Max runtime datasource pools | No |
| `CORE_POSTGRES_POOL_MAX_SIZE` | Max connections per runtime pool | No |
| `CORE_POSTGRES_POOL_MIN_IDLE` | Min idle connections per runtime pool | No |
| `CORE_POSTGRES_POOL_IDLE_EVICTION_MS` | Idle eviction window | No |
| `CORE_POSTGRES_POOL_CLEANUP_INTERVAL_MS` | Pool cleanup schedule | No |
| `CORE_POSTGRES_POOL_BORROW_TIMEOUT_MS` | Pool borrow timeout | No |

The checked-in `env.properties` file is a local development configuration, not a production deployment contract.

### 3. Run the backend

```bash
mvn spring-boot:run
```

The API starts on `http://localhost:8080` unless `SERVER_PORT` is overridden.

### 4. Run the frontend

From the sibling `../frontend` app:

```bash
npm install
npm run dev
```

The Vite dev server runs on `http://localhost:5173`. By default it proxies `/api` to `http://localhost:8080`, which matches the backend CORS configuration.

If you need a direct frontend API base URL, set `VITE_API_BASE_URL`. Otherwise the frontend defaults to `/api`.

### Docker status

Containerization currently covers the local PostgreSQL dependency only. There is no committed backend Dockerfile or full multi-service Compose stack for backend + frontend packaging in this module yet.

## API and Workflow Overview

The intended user flow is:

1. Create an account with `POST /account/signup`.
2. Log in with `POST /account/login`.
3. Create a saved datasource with `POST /datasource`.
4. Test the runtime PostgreSQL connection with `POST /datasource/{id}/test-connection`.
5. Generate a schema snapshot with `POST /core/datasources/{datasourceId}/schema/generate`.
6. Browse schema and tables through:
   - `GET /core/datasources/{datasourceId}/schema`
   - `GET /core/datasources/{datasourceId}/tables`
   - `GET /core/datasources/{datasourceId}/tables/{tableName}`
   - `GET /core/datasources/{datasourceId}/tables/{tableName}/columns`
   - `GET /core/datasources/{datasourceId}/tables/{tableName}/relations`
7. Query or mutate data through:
   - `GET /core/datasources/{datasourceId}/tables/{tableName}/rows`
   - `POST /core/datasources/{datasourceId}/tables/{tableName}/aggregate`
   - `POST /core/datasources/{datasourceId}/tables/{tableName}/rows`
   - `PATCH /core/datasources/{datasourceId}/tables/{tableName}/rows/{id}`
   - `DELETE /core/datasources/{datasourceId}/tables/{tableName}/rows/{id}`

Two details matter in practice:

- Table identifiers may be passed as plain names only when they resolve uniquely. If the same table name exists in multiple schemas, the API requires a schema-qualified name such as `public.orders`.
- The backend `/core/...` workflow is implemented today. The current frontend uses the backend for authentication and attempts datasource onboarding, but the playground remains a presentation-oriented preview with static schema and mocked query execution.

## Testing

Automated test coverage is still thin in the current repository state:

- There are no committed backend test classes under `src/test`.
- The frontend does not currently define a test runner script.

Practical verification paths right now are:

- Backend build/test phase:

```bash
mvn test
```

- Frontend quality/build checks:

```bash
npm run lint
npm run build
```

Integration testing against live PostgreSQL instances is clearly anticipated by the code structure, but it has not been committed yet as a formal test suite.

## Design Decisions

### Why PostgreSQL-only for now

The current engine is intentionally dialect-specific. It validates that the runtime target is actually PostgreSQL, builds PostgreSQL JDBC properties, introspects PostgreSQL catalogs, models PostgreSQL column semantics, and generates PostgreSQL SQL. That is a stronger position than a shallow multi-database abstraction, especially for a project that needs to be defensible in a competition or presentation setting.

### Why the runtime is schema-driven

ForgeQL is meant for databases that are not known at compile time. A generated schema snapshot lets the engine resolve identifiers from trusted metadata, compute read/aggregate/mutation capability per object, and apply type-aware validation before SQL is built. Without that schema layer, the system would either become brittle or drift toward unsafe raw SQL handling.

### Why composite-key and no-PK tables are read/aggregate only in v1

The current mutation routes are intentionally simple: one row, one primary key value, one deterministic `WHERE pk = ?`. Tables without a primary key, or with composite primary keys, do not fit that contract cleanly. The code still introspects them and exposes them for browsing, reads, and aggregates, but leaves mutation support disabled until a more explicit identity model is introduced.

### Why the safety restrictions are strict

This engine is dynamic by design, which makes validation more important, not less. The current implementation deliberately:

- never accepts raw SQL from clients
- resolves tables from generated schema only
- quotes identifiers explicitly
- binds values as prepared-statement parameters
- rejects unsupported mutation target types
- blocks writes to generated and identity columns
- constrains update/delete to primary-key scope
- classifies PostgreSQL failures into stable API errors

One important implementation note: pagination is validated structurally, but the code does not yet enforce a hard upper bound for `limit`. That is a likely hardening step rather than an advertised feature today.

## Future Work

- Add a real dialect abstraction for MySQL and other engines without weakening the current PostgreSQL path.
- Expand the query language with richer boolean logic, deeper relation traversal, and more expressive projections.
- Add optimistic concurrency for mutations through explicit version or timestamp preconditions.
- Persist schema snapshots and improve cache invalidation beyond the current in-memory registry.
- Broaden PostgreSQL integration tests across local, containerized, and managed-service style environments.
- Finish the frontend integration so live schema generation, browsing, querying, and mutation use the backend `/core` endpoints instead of preview data.
- Package the backend and frontend with production-ready containerization, secure cookie settings, and deployment-oriented environment separation.
