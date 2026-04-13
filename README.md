# ForgeQL

ForgeQL is a schema-driven PostgreSQL exploration and execution engine with a Spring Boot backend and a React frontend. It is built for the case where the application does not know the target database schema at compile time, but still needs a safe and structured way to connect, inspect, query, aggregate, and mutate data. Instead of relying on hardcoded entity models, ForgeQL generates a runtime schema snapshot from live PostgreSQL metadata and uses that snapshot as the source of truth for execution.

## What Problem ForgeQL Solves

Dynamic database access is difficult to do safely. Once a system allows users to connect arbitrary databases at runtime, it can no longer depend on compile-time entity classes, static ORM mappings, or trusted table and column names from the client.

ForgeQL addresses that by making execution schema-driven. It connects to a user-managed PostgreSQL datasource, validates that the target is actually PostgreSQL, introspects the live schema, and then uses the generated schema snapshot to validate reads, aggregates, and mutations before SQL is built. That gives the system flexibility without dropping into unsafe raw SQL execution.

## Main Features

- Account signup, login, logout, and token refresh flow
- Saved datasource registration with encrypted stored passwords
- PostgreSQL connection testing before runtime use
- Live schema generation from PostgreSQL metadata
- Schema browsing for tables, columns, and relations
- Row reads with projection, filters, sorting, and pagination
- Aggregate queries with validated functions and optional grouping
- Controlled insert, update, and delete operations
- Runtime capability model based on real table shape and keys
- PostgreSQL-specific type validation and coercion
- Lazy runtime connection pooling with bounded resource limits

## High-Level Architecture

ForgeQL has four main parts:

- `frontend`: the user-facing React application for authentication, datasource onboarding, and exploration
- `backend`: the Spring Boot application that owns auth, datasource persistence, schema generation, planning, SQL generation, and execution
- application database: the PostgreSQL database used by ForgeQL itself for users, saved datasources, and refresh tokens
- user-managed PostgreSQL datasources: external databases that users register and query through the engine

The execution path is schema-driven:

```text
saved datasource
-> runtime connection resolution
-> PostgreSQL validation
-> schema introspection
-> generated schema
-> AST
-> validated plan
-> SQL
-> execution
-> API response
```

The important design choice is that generated schema metadata becomes the contract for execution. Clients do not send raw SQL, and they do not directly control trusted identifiers.

## Repository Structure

```text
ForgeQL/
|-- backend/
|   |-- pom.xml
|   `-- src/
|-- frontend/
|   |-- package.json
|   `-- src/
|-- docker/
|   |-- backend.Dockerfile
|   `-- frontend.Dockerfile
|-- scripts/
|   |-- build.sh
|   |-- clean.sh
|   |-- run.sh
|   `-- test.sh
|-- .env.example
|-- docker-compose.yml
`-- README.md
```

## Backend Overview

The backend owns the core runtime logic of the project:

- account authentication and token lifecycle
- datasource persistence and ownership checks
- encryption of saved datasource passwords
- PostgreSQL connection validation and runtime connection building
- schema generation and schema registry lifecycle
- read, aggregate, and mutation planning and execution
- capability enforcement based on live metadata
- API error shaping and PostgreSQL-aware failure classification
- lazy runtime datasource pooling with max-pool limits and idle eviction

At a high level, the backend turns a saved datasource into a validated execution surface rather than treating it as a raw JDBC endpoint.

## Frontend Overview

The frontend provides the user-facing application flow:

- signup and login
- datasource creation and connection testing
- schema generation workflow
- schema and table exploration UI
- runtime exploration interface for reads, aggregates, and mutations

The backend integration is real for authentication and datasource flows. The broader exploration experience is implemented, but parts of the frontend are still presentation-oriented and not yet as mature as the backend execution engine.

## Supported Functionality

### Capability model

ForgeQL computes object capabilities from live schema metadata:

| Object type | Read | Aggregate | Insert | Update | Delete |
| --- | --- | --- | --- | --- | --- |
| `TABLE` with exactly one PK column | Yes | Yes | Yes | Yes | Yes |
| `TABLE` with no PK | Yes | Yes | No | No | No |
| `TABLE` with composite PK | Yes | Yes | No | No | No |
| `VIEW` | Yes | Yes | No | No | No |
| `MATERIALIZED_VIEW` | Yes | Yes | No | No | No |

### Reads

- explicit projection
- validated filters
- validated sort fields
- `limit` and `offset`
- schema-qualified execution when needed

Supported filter operators:

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

### Aggregates

- `count`
- `sum`
- `avg`
- `min`
- `max`
- optional `groupBy`
- validated field and function compatibility

### Mutations

- insert, update, and delete are supported only for real tables with exactly one primary key column
- update and delete are primary-key scoped
- generated, identity, and non-writable columns are blocked
- unsupported mutation targets are rejected explicitly

### PostgreSQL-specific validation

Current schema-aware coercion and validation covers:

- booleans
- integer types
- numeric values
- text-like values
- UUID
- enums
- JSON / JSONB
- arrays
- timestamp types

### Runtime pooling

- pools are created lazily on first runtime use
- pools are keyed by datasource and connection fingerprint
- active pool count is bounded
- idle pools are evicted on a schedule

## API / Workflow Overview

The main project workflow is:

1. Sign up or log in
2. Create a saved datasource
3. Test the PostgreSQL connection
4. Generate a schema snapshot
5. Browse the schema and tables
6. Read rows
7. Run aggregates
8. Create, update, or delete rows where the table capability model allows it

Important backend endpoints include:

- `POST /account/signup`
- `POST /account/login`
- `POST /datasource`
- `POST /datasource/{id}/test-connection`
- `POST /core/datasources/{datasourceId}/schema/generate`
- `GET /core/datasources/{datasourceId}/schema`
- `GET /core/datasources/{datasourceId}/tables`
- `GET /core/datasources/{datasourceId}/tables/{tableName}/columns`
- `GET /core/datasources/{datasourceId}/tables/{tableName}/relations`
- `GET /core/datasources/{datasourceId}/tables/{tableName}/rows`
- `POST /core/datasources/{datasourceId}/tables/{tableName}/aggregate`
- `POST /core/datasources/{datasourceId}/tables/{tableName}/rows`
- `PATCH /core/datasources/{datasourceId}/tables/{tableName}/rows/{id}`
- `DELETE /core/datasources/{datasourceId}/tables/{tableName}/rows/{id}`

## Running the Project

### Full Docker flow

From the repository root:

```bash
cp .env.example .env
./scripts/build.sh
./scripts/test.sh
./scripts/run.sh
```

Default published ports:

- frontend: `http://localhost:3000`
- backend: `http://localhost:8080`
- application database: `localhost:5434`

### Local development without full Docker

Run PostgreSQL in Docker, then run backend and frontend locally:

```bash
docker compose up -d postgres
cd backend
mvn spring-boot:run
cd ../frontend
npm ci
npm run dev
```

The Vite dev server runs on `http://localhost:5173` and proxies `/api` to `http://localhost:8080`.

### Scripts

- `scripts/build.sh`: builds the backend jar, frontend bundle, and Docker images
- `scripts/test.sh`: runs backend and frontend tests
- `scripts/run.sh`: starts the Docker Compose stack and waits for service readiness
- `scripts/clean.sh`: stops the stack and prunes dangling Docker images

## Configuration

The project is environment-driven. Key variables include:

- `APP_DATASOURCE_URL`
- `APP_DATASOURCE_USERNAME`
- `APP_DATASOURCE_PASSWORD`
- `APP_DATASOURCE_DRIVER`
- `DATASOURCE_ENCRYPTION_SECRET`
- `JWT_SECRET`
- `SERVER_PORT`
- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `CORE_POSTGRES_POOL_MAX_ACTIVE_POOLS`
- `CORE_POSTGRES_POOL_MAX_SIZE`
- `CORE_POSTGRES_POOL_MIN_IDLE`
- `CORE_POSTGRES_POOL_IDLE_EVICTION_MS`
- `CORE_POSTGRES_POOL_CLEANUP_INTERVAL_MS`
- `CORE_POSTGRES_POOL_BORROW_TIMEOUT_MS`

Use `.env.example` as the template for Docker-based runs.

## Current Project Status

What is already strong:

- the backend execution path is implemented around a real PostgreSQL runtime model
- schema generation, browsing, reads, aggregates, and mutation capability checks are present
- the codebase includes integration-style backend tests using Testcontainers
- the repository now has a full Docker-based local stack and simple script-based build/test/run flow

What is still incomplete or needs polishing:

- the frontend is behind the backend in depth and completeness
- some frontend areas remain presentation-oriented compared with the backend execution engine
- local developer experience still depends on consistent environment setup, especially for database credentials and secrets
- there is room to tighten documentation, run configuration consistency, and production hardening

## Future Work

- finish deeper frontend integration for the runtime exploration flow
- expand integration testing across more PostgreSQL scenarios
- harden local and production configuration consistency
- improve schema persistence and cache lifecycle beyond the current in-memory approach
- extend validation and execution capabilities while preserving the schema-driven safety model
- consider broader database support later without weakening the PostgreSQL-first core
