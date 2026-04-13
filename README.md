# ForgeQL

ForgeQL is a PostgreSQL-focused dynamic backend engine with a React frontend. It lets users register PostgreSQL datasources at runtime, generate schema snapshots from live metadata, and work with those schemas through validated read, aggregate, and mutation flows.

The repository is organized by responsibility:

- `backend/` contains the Spring Boot application and backend-only configuration.
- `frontend/` contains the Vite/React application.
- `docker/`, `docker-compose.yml`, `.env.example`, and `scripts/` are repository-level infrastructure.

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

## Core Runtime

The backend owns:

- authentication and account management
- saved datasource management
- runtime PostgreSQL connection validation
- schema generation from PostgreSQL metadata
- schema browsing, row reads, aggregates, and controlled mutations

The frontend provides the user-facing flow for authentication, datasource onboarding, and interactive exploration.

## Docker and Pipeline

### Full stack from scratch

```bash
cp .env.example .env
./scripts/build.sh
./scripts/test.sh
./scripts/run.sh
```

Default published endpoints:

- frontend: `http://localhost:3000`
- backend: `http://localhost:8080`
- application database: `localhost:5434`

### Script pipeline

- `scripts/build.sh`: builds the backend jar, frontend assets, and Docker images
- `scripts/test.sh`: runs backend and frontend tests
- `scripts/run.sh`: starts the Docker Compose stack and waits for healthy services
- `scripts/clean.sh`: stops the stack and prunes dangling Docker images

The intended flow is `build -> test -> run`.

### Container topology

- `postgres` stores ForgeQL application state in a named Docker volume
- `backend` connects to `postgres` over the internal Docker network
- `frontend` serves the production React build through Nginx
- Nginx proxies `/api` traffic to `backend`, so browser traffic stays same-origin in the containerized setup

## Local Development Without Full Docker

Run only PostgreSQL in Docker, then start backend and frontend locally:

```bash
docker compose up -d postgres
cd backend
mvn spring-boot:run
cd ../frontend
npm ci
npm run dev
```

The Vite dev server runs on `http://localhost:5173` and proxies `/api` to `http://localhost:8080`.

## Backend Configuration

The backend is environment-driven. The main variables are:

- `APP_DATASOURCE_URL`
- `APP_DATASOURCE_USERNAME`
- `APP_DATASOURCE_PASSWORD`
- `APP_DATASOURCE_DRIVER`
- `DATASOURCE_ENCRYPTION_SECRET`
- `JWT_SECRET`
- `SERVER_PORT`
- `CORE_POSTGRES_POOL_MAX_ACTIVE_POOLS`
- `CORE_POSTGRES_POOL_MAX_SIZE`
- `CORE_POSTGRES_POOL_MIN_IDLE`
- `CORE_POSTGRES_POOL_IDLE_EVICTION_MS`
- `CORE_POSTGRES_POOL_CLEANUP_INTERVAL_MS`
- `CORE_POSTGRES_POOL_BORROW_TIMEOUT_MS`

Use `.env.example` as the repository-level configuration template for Docker-based runs.
