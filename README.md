# ForgeQL

ForgeQL is a self-hosted PostgreSQL data workspace with a Spring Boot backend and a React/TypeScript frontend. It lets teams register PostgreSQL datasources, generate a runtime schema snapshot, browse tables and relations, and perform validated row-level CRUD operations without exposing raw SQL to the client.

## Core Features

- Local self-hosted deployment with Docker Compose
- `MAIN_ADMIN` bootstrap on first startup
- Global RBAC with `MAIN_ADMIN`, `ADMIN`, `MEMBER`, and `VIEWER`
- Datasource-level access with `MANAGER` and `VIEWER`
- PostgreSQL datasource creation, update, deletion, and connection testing
- Runtime schema generation and schema browsing
- Table exploration, filtering, sorting, pagination, and aggregates
- Row-level create, update, and delete operations where table capabilities allow them
- PostgreSQL-only runtime engine
- No raw SQL execution from the frontend

## Architecture Overview

ForgeQL is split into four main parts:

- `backend/`: Spring Boot API for auth, RBAC, datasource persistence, schema generation, and runtime execution
- `frontend/`: React + TypeScript app for authentication, datasource management, schema browsing, and row operations
- Application PostgreSQL database: stores ForgeQL users, refresh tokens, datasources, and datasource access assignments
- Registered PostgreSQL datasources: external databases explored through ForgeQL's runtime engine

The runtime flow is:

```text
registered datasource
-> RBAC check
-> PostgreSQL connection resolution
-> schema generation / schema lookup
-> validated runtime request
-> SQL generation inside the backend
-> PostgreSQL execution
-> API response
```

## RBAC

ForgeQL applies RBAC in two layers.

### Global roles

- `MAIN_ADMIN`: full system access, the only role that can create, update, and delete datasources globally, and the only role that can administer every datasource
- `ADMIN`: can manage users, can promote `MEMBER` and `VIEWER` users to `ADMIN`, cannot modify `MAIN_ADMIN`, cannot demote another `ADMIN`, and can manage datasource access only for datasources they own
- `MEMBER`: standard workspace user; if assigned datasource `MANAGER` access they can manage and write within that datasource according to datasource-level rules
- `VIEWER`: read-only workspace user

### Datasource roles

- `MANAGER`: datasource-level management and runtime write access where the generated schema allows mutations
- `VIEWER`: datasource-level read access only

### Access model summary

- Unassigned users cannot access a datasource
- Datasource owners retain `MANAGER` access to their own datasource
- `VIEWER` users remain read-only
- The client never sends raw SQL; all reads and mutations are validated by the backend

## MAIN_ADMIN Bootstrap Flow

On startup, the backend checks whether a `MAIN_ADMIN` already exists.

- If one exists, bootstrap is skipped
- If none exists, the backend creates the initial `MAIN_ADMIN` from environment variables
- ForgeQL requires exactly one `MAIN_ADMIN`

Required bootstrap variables:

- `INITIAL_MAIN_ADMIN_EMAIL`
- `INITIAL_MAIN_ADMIN_PASSWORD`
- `INITIAL_MAIN_ADMIN_USERNAME`

These values can be provided through the root `.env` file or exported environment variables.

## Datasource Setup

ForgeQL currently supports PostgreSQL datasources only.

Typical datasource flow:

1. Start ForgeQL and sign in as the bootstrapped `MAIN_ADMIN`
2. Create a datasource from the datasource catalog
3. Provide PostgreSQL connection details
4. Test the connection
5. Generate the schema snapshot
6. Browse tables and run row-level operations through the explorer
7. Assign datasource access to other users when needed

Datasource records store connection metadata in the ForgeQL app database. Stored datasource passwords are encrypted by the backend.

## Required Environment Variables

The repo already includes a root `.env` file used by local Docker Compose runs. These are the active variables in the current project:

### Application database

- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `POSTGRES_PORT`
- `APP_DATASOURCE_URL`
- `APP_DATASOURCE_USERNAME`
- `APP_DATASOURCE_PASSWORD`
- `APP_DATASOURCE_DRIVER`

### MAIN_ADMIN bootstrap

- `INITIAL_MAIN_ADMIN_EMAIL`
- `INITIAL_MAIN_ADMIN_PASSWORD`
- `INITIAL_MAIN_ADMIN_USERNAME`

### Backend secrets and runtime config

- `DATASOURCE_ENCRYPTION_SECRET`
- `JWT_SECRET`
- `SERVER_PORT`
- `CORE_POSTGRES_POOL_MAX_ACTIVE_POOLS`
- `CORE_POSTGRES_POOL_MAX_SIZE`
- `CORE_POSTGRES_POOL_MIN_IDLE`
- `CORE_POSTGRES_POOL_IDLE_EVICTION_MS`
- `CORE_POSTGRES_POOL_CLEANUP_INTERVAL_MS`
- `CORE_POSTGRES_POOL_BORROW_TIMEOUT_MS`

### Frontend / reverse proxy

- `FRONTEND_PORT`
- `VITE_API_BASE_URL`
- `BACKEND_UPSTREAM`
- `BACKEND_IMAGE`
- `FRONTEND_IMAGE`

## Running Backend Tests

From `backend/`:

```bash
mvn test
```

This runs the Spring Boot test suite, including PostgreSQL integration tests backed by Testcontainers.

## Running Frontend Tests

From `frontend/`:

```bash
npm test
```

The frontend tests run with Vitest and React Testing Library.

## Running with Docker Compose

From the repo root:

```bash
docker compose up -d --build
```

Default published ports:

- Frontend: `http://localhost:3000`
- Backend: `http://localhost:8080`
- Application PostgreSQL database: `localhost:5434`

The repo also includes helper scripts:

```bash
./scripts/build.sh
./scripts/test.sh
./scripts/run.sh
```

- `scripts/build.sh`: builds backend, frontend, and Docker images
- `scripts/test.sh`: runs backend and frontend tests
- `scripts/run.sh`: starts the Docker Compose stack and waits for health checks

## Self-Hosted Local Setup

For a standard local run:

1. Review the root `.env` values
2. Start the stack with Docker Compose
3. Open the frontend on `http://localhost:3000`
4. Sign in with the bootstrapped `MAIN_ADMIN`
5. Register PostgreSQL datasources and generate schema snapshots

ForgeQL is intended for self-hosted local or team-managed deployment, with the backend and frontend talking only to the services defined in the Compose stack or the PostgreSQL datasources you explicitly register.
