# PostgreSQL Core Engine Blueprint

## 1. Purpose

For the April 26 competition build, the core engine should target **PostgreSQL only** and aim to be a **fully working PostgreSQL integration**, not an MVP demo.

This module must:

- connect to most real PostgreSQL databases reachable by JDBC
- inspect structure dynamically
- generate a runtime schema snapshot
- expose schema browsing
- expose row exploration
- expose aggregates
- expose record mutations
- provide an AST -> validated plan -> SQL execution pipeline

Out of scope for this competition build:

- MySQL support
- MariaDB support
- generic multi-dialect implementation

Future multi-engine support can still be added later, but this document assumes the engine being built now is a robust PostgreSQL engine.

## 2. PostgreSQL Compatibility Target

The competition version should aim to work with most PostgreSQL installations that expose standard JDBC access, including:

- local PostgreSQL
- Docker PostgreSQL
- cloud-managed PostgreSQL with normal TCP access
- providers such as Supabase, Neon, Railway, Render, and Amazon RDS, as long as standard JDBC connection info is available

Recommended runtime support target:

- PostgreSQL 13+
- standard schemas and custom user schemas
- standard tables
- views for read and aggregate operations
- materialized views for read and aggregate operations if metadata access is available

Not required for the first production-capable build:

- SSH tunnel orchestration
- Postgres extension-specific query semantics
- exotic non-tabular objects as first-class mutation targets

## 3. Architecture

The module should be built as a schema-driven PostgreSQL runtime:

1. `Datasource Runtime Layer`
   Resolves one saved datasource and produces a runtime connection definition.

2. `Postgres Connection Layer`
   Builds JDBC connections with SSL, timeout, and session options.

3. `Postgres Introspection Layer`
   Reads metadata from `information_schema` and `pg_catalog`.

4. `Schema Modeling Layer`
   Converts raw metadata into a normalized internal schema model.

5. `Schema Registry Layer`
   Stores generated schema snapshots per datasource.

6. `AST Layer`
   Converts incoming requests into internal read, aggregate, and mutation ASTs.

7. `Planning Layer`
   Validates AST nodes against the schema and produces execution plans.

8. `SQL Layer`
   Generates safe PostgreSQL SQL from validated plans.

9. `Execution Layer`
   Runs the SQL through JDBC and maps results.

10. `API Layer`
   Exposes schema, exploration, aggregate, and mutation endpoints.

Internal flow:

`saved datasource -> connection config -> postgres introspection -> generated schema -> request AST -> validated plan -> postgres SQL -> JDBC execution -> response mapping`

## 4. Full Workflow

### 4.1 Datasource To Runtime Connection

1. The user selects one saved datasource.
2. The backend loads the datasource owned by that user.
3. The backend decrypts the stored password.
4. The backend builds a PostgreSQL JDBC URL from host, port, database, schema, SSL mode, timeout settings, and optional connection properties.
5. The backend verifies that the target product is actually PostgreSQL.
6. The backend verifies connectivity before schema generation or query execution.

### 4.2 Schema Generation

1. Open an introspection connection.
2. Determine server version and effective schema scope.
3. Discover all supported objects in allowed schemas:
   tables, views, and optionally materialized views.
4. Load columns with:
   name, type, nullability, default, ordinal position, identity, generated status, precision, scale, length.
5. Load primary keys.
6. Load unique constraints.
7. Load foreign keys and relation direction.
8. Load enum metadata where relevant.
9. Build the internal schema snapshot.
10. Validate the snapshot.
11. Compute a fingerprint.
12. Cache and optionally persist the snapshot.

### 4.3 Read Exploration

1. Request targets datasource + table.
2. Table resolves through generated schema.
3. Read AST is built.
4. Planner validates columns, filters, sort, pagination, and includes later.
5. SQL builder creates safe `SELECT` SQL with explicit column list.
6. Executor runs query with limit and offset bounds.
7. Response returns rows plus metadata.

### 4.4 Aggregate Flow

1. Request targets datasource + table.
2. Aggregate AST is built with operations such as `count`, `sum`, `avg`, `min`, `max`.
3. Planner validates target fields and function compatibility.
4. SQL builder creates aggregate SQL.
5. Executor returns typed aggregate result.

### 4.5 Insert Flow

1. Request targets datasource + table.
2. Insert AST is built from client payload.
3. Planner validates writable columns, required fields, type coercion, and generated columns.
4. SQL builder creates `INSERT ... RETURNING ...`.
5. Executor runs inside a transaction.
6. Response returns created row or created identifier.

Mutation identity policy for v1:

- full mutation support is enabled only for tables with exactly one primary key column
- tables with composite primary keys are still introspected and can be read or aggregated, but mutation capability is disabled in v1
- tables without a primary key are read and aggregate only in v1

### 4.6 Update Flow

1. Request targets datasource + table + primary key.
2. Update AST is built.
3. Planner validates writable columns, PK scope, and value coercion.
4. SQL builder creates `UPDATE ... WHERE pk = ? RETURNING ...`.
5. Executor runs inside a transaction.
6. Response returns updated row or affected count.

### 4.7 Delete Flow

1. Request targets datasource + table + primary key.
2. Delete AST is built.
3. Planner validates scope.
4. SQL builder creates `DELETE ... WHERE pk = ? RETURNING ...`.
5. Executor runs inside a transaction.
6. Response returns deleted identity or affected count.

Mandatory safety rule for v1:

- no unrestricted mass update
- no unrestricted mass delete
- no mutation path for composite primary key tables

## 5. Main Components

### Connection

- `PostgresConnectionPropertiesFactory`
- `PostgresConnectionFactory`
- `PostgresConnectionTestService`
- `PostgresConnectionValidator`

### Introspection

- `PostgresMetadataIntrospector`
- `TableMetadataLoader`
- `ColumnMetadataLoader`
- `PrimaryKeyMetadataLoader`
- `UniqueConstraintMetadataLoader`
- `ForeignKeyMetadataLoader`
- `EnumMetadataLoader`

### Schema

- `SchemaGenerationService`
- `SchemaAssembler`
- `SchemaValidationService`
- `SchemaFingerprintService`
- `SchemaSerializationService`
- `SchemaRegistryService`

### AST And Planning

- `ReadQueryAstBuilder`
- `AggregateQueryAstBuilder`
- `MutationAstBuilder`
- `ReadQueryPlanner`
- `AggregateQueryPlanner`
- `InsertPlanner`
- `UpdatePlanner`
- `DeletePlanner`
- `ExecutionPlanValidator`

### SQL And Execution

- `PostgresIdentifierQuoter`
- `ReadSqlBuilder`
- `AggregateSqlBuilder`
- `InsertSqlBuilder`
- `UpdateSqlBuilder`
- `DeleteSqlBuilder`
- `JdbcRowExecutor`
- `AggregateExecutor`
- `MutationExecutor`
- `ValueCoercionService`

### API

- `SchemaGenerationController`
- `SchemaReadController`
- `TableExploreController`
- `AggregateController`
- `MutationController`

## 6. Schema Representation Design

The internal schema must be the source of truth for all dynamic operations.

Recommended table object shape:

```json
{
  "name": "orders",
  "schema": "public",
  "qualifiedName": "public.orders",
  "tableType": "TABLE",
  "primaryKey": {
    "columns": ["id"]
  },
  "uniqueConstraints": [
    {
      "name": "uk_orders_number",
      "columns": ["order_number"]
    }
  ],
  "columns": [
    {
      "name": "id",
      "dbType": "int8",
      "javaType": "Long",
      "nullable": false,
      "identity": true,
      "generated": false,
      "defaultValue": null,
      "position": 1,
      "writable": false,
      "filterable": true,
      "sortable": true,
      "aggregatable": true
    }
  ],
  "relations": [],
  "capabilities": {
    "read": true,
    "aggregate": true,
    "insert": true,
    "update": true,
    "delete": true
  }
}
```

The schema should also store:

- datasource id
- server version
- generated timestamp
- default schema
- fingerprint
- supported table list
- relation graph

For PostgreSQL specifically, include enough metadata to reason about:

- identity columns
- generated columns
- enums
- JSON and JSONB
- arrays
- timestamp vs timestamptz
- numeric precision and scale

## 7. Supported Object Rules

First serious PostgreSQL build should support:

- tables:
  read, aggregate, mutate

- views:
  read and aggregate
  no mutation by default

- materialized views:
  read and aggregate
  no mutation by default

Unsupported objects should be explicitly excluded or marked unsupported in schema output.

Capability computation rules:

- `TABLE` with exactly one primary key column:
  `read=true`, `aggregate=true`, `insert=true`, `update=true`, `delete=true`

- `TABLE` with no primary key or composite primary key:
  `read=true`, `aggregate=true`, `insert=false`, `update=false`, `delete=false` in v1

- `VIEW`:
  `read=true`, `aggregate=true`, `insert=false`, `update=false`, `delete=false`

- `MATERIALIZED_VIEW`:
  `read=true`, `aggregate=true`, `insert=false`, `update=false`, `delete=false`

- generated columns, identity columns, and computed columns must be marked `writable=false` even when the table itself is writable

## 8. API Endpoints

### Schema

- `POST /core/datasources/{datasourceId}/schema/generate`
- `POST /core/datasources/{datasourceId}/schema/refresh`
- `GET /core/datasources/{datasourceId}/schema`
- `GET /core/datasources/{datasourceId}/schema/summary`

### Schema Browsing

- `GET /core/datasources/{datasourceId}/tables`
- `GET /core/datasources/{datasourceId}/tables/{tableName}`
- `GET /core/datasources/{datasourceId}/tables/{tableName}/columns`
- `GET /core/datasources/{datasourceId}/tables/{tableName}/relations`

### Reads

- `GET /core/datasources/{datasourceId}/tables/{tableName}/rows`

### Aggregates

- `POST /core/datasources/{datasourceId}/tables/{tableName}/aggregate`

### Mutations

- `POST /core/datasources/{datasourceId}/tables/{tableName}/rows`
- `PATCH /core/datasources/{datasourceId}/tables/{tableName}/rows/{id}`
- `DELETE /core/datasources/{datasourceId}/tables/{tableName}/rows/{id}`

Primary key routing policy for v1:

- these mutation endpoints are valid only for tables with a single-column primary key
- composite primary key tables must not expose these endpoints as writable resources in v1

## 9. Safe Dynamic Access Rules

- never execute raw SQL from clients
- never trust client-provided identifiers directly
- resolve table and column names from generated schema only
- always quote identifiers with PostgreSQL rules
- always bind values as JDBC parameters
- never use `SELECT *`
- cap pagination
- mutation targets must be schema-approved and writable
- aggregate fields must be schema-approved and aggregatable
- update and delete must be primary-key-scoped in v1
- always qualify tables with schema, do not rely on `search_path`

Public table identifier policy:

- internal execution must always use fully qualified table names
- public API may accept plain table names only when they resolve uniquely inside the allowed schema scope
- if the same table name exists in multiple allowed schemas, the request must fail with validation error and require a schema-qualified identifier such as `public.orders`

Supported filter language for the competition build:

- top-level fields are combined with `AND`
- multiple operators on the same field are combined with `AND`
- no arbitrary nested boolean trees in v1
- no `OR` support in v1

Supported operators:

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

Operator rules:

- `in` requires a non-empty array
- `between` requires exactly two values
- `like` and `ilike` require a string pattern
- `isNull` and `isNotNull` do not accept arbitrary value payloads beyond a boolean-style enablement

## 10. Validation And Security Rules

### Connection Rules

- datasource must exist and belong to caller
- connection must succeed before schema generation
- actual server product must be PostgreSQL
- server version should be checked and stored

### PostgreSQL Schema Scope Rules

Exclude by default:

- `pg_catalog`
- `information_schema`
- `pg_toast`
- temporary schemas such as `pg_temp_*`

### Query Rules

- hard max `limit`
- non-negative `offset`
- only known columns in filters and sorts
- supported operators only
- aggregate function and field compatibility checks
- mutation payload type coercion checks

### Type Coercion Rules

- boolean columns accept boolean input only
- smallint, integer, and bigint inputs must be numeric and within PostgreSQL range for the target type
- numeric and decimal inputs must be checked against declared precision and scale when metadata is available
- UUID columns must receive a valid canonical UUID string
- enum columns must match one of the discovered PostgreSQL enum labels
- json and jsonb columns must receive valid JSON values
- array columns must receive JSON-array-style input and every element must be coercible to the PostgreSQL base type
- `timestamp with time zone` should require ISO-8601 input with offset or `Z`
- `timestamp without time zone` should use timestamp input without timezone conversion assumptions
- null assignment is allowed only for nullable columns
- unsupported or not-yet-mapped PostgreSQL mutation types must be rejected explicitly instead of being silently string-cast

### Mutation Rules

- no writes to generated columns
- no writes to identity columns unless explicitly supported
- required non-null columns must be present on insert if they have no default
- update payload cannot be empty
- delete and update require PK scope in first version
- v1 mutation concurrency policy is `last-write-wins`
- optimistic concurrency is not required for the competition build
- if optimistic concurrency is added later, it should be implemented through explicit version or updated-at preconditions, not hidden behavior

### Transaction Rules

- one mutation request equals one database transaction
- if any validation, SQL execution, or result-mapping failure occurs after transaction start, the whole mutation request rolls back
- insert and update responses that return row data must do so from the same transaction before commit
- read and aggregate requests are non-mutating single-operation executions and do not require multi-step transaction orchestration in v1

### Error Taxonomy

Recommended error categories and response intent:

- connection failure to target PostgreSQL database:
  service unavailable style error

- authentication or SSL failure against target PostgreSQL database:
  bad gateway or service unavailable style error, with safe message and no secret leakage

- timeout while connecting or executing:
  gateway timeout style error

- schema generation failure:
  internal or upstream database error depending on root cause

- validation failure:
  bad request

- unsupported object, unsupported capability, unsupported type, or unsupported mutation target:
  unprocessable entity

- datasource, table, or row not found:
  not found

- unique constraint violation and conflicting mutation state:
  conflict

- foreign key violation on delete or update:
  conflict

- unexpected PostgreSQL or JDBC failure:
  internal server error

Every error response from this module should include:

- HTTP status
- stable application error code
- safe human-readable message
- datasource id and target path only when safe to expose

### Operational Security

- decrypt password only when building runtime connection
- never log secrets
- apply connection timeout
- apply statement timeout
- wrap writes in transactions
- map JDBC and PostgreSQL errors to clean API errors

### Pooling And Resource Policy

- do not create permanent pools for every saved datasource eagerly
- create datasource pools lazily on first runtime use
- keep pools keyed by datasource id and PostgreSQL connection fingerprint
- enforce a bounded maximum number of active pools in the process
- evict and close idle pools after a configured inactivity window
- keep per-datasource pool size intentionally small in the competition build so one noisy datasource cannot exhaust the whole backend

## 11. PostgreSQL Connection Coverage Requirements

To claim the app works with most PostgreSQL databases, the datasource model and runtime connection builder should support:

- host
- port
- database name
- schema name
- username
- password
- SSL mode
- SSL root certificate or trust material reference for `verify-ca` and `verify-full`
- connect timeout
- socket timeout
- application name
- optional extra JDBC options

Recommended PostgreSQL SSL modes to support:

- `disable`
- `prefer`
- `require`
- `verify-ca`
- `verify-full`

The runtime builder should not hardcode local assumptions.

## 12. Datasource Model Requirements

The current datasource CRUD is a good start, but for a fully working PostgreSQL engine the saved datasource needs to represent runtime reality.

Minimum required saved fields:

- id
- owner user id
- display name
- db type
- host
- port
- database name
- schema name
- username
- encrypted password
- ssl mode
- status
- created at
- updated at

Recommended additional fields:

- `connectTimeoutMs`
- `socketTimeoutMs`
- `applicationName`
- `sslRootCertRef`
- `lastConnectionTestAt`
- `lastConnectionStatus`
- `lastConnectionError`
- `lastSchemaGeneratedAt`
- `lastSchemaFingerprint`
- `serverVersion`
- `extraJdbcOptionsJson`

Important modeling rule:

- request DTO should send plain `password`
- service layer should encrypt before persistence

## 13. Current Backend Scan And What Must Change

### Good Starting Points

- auth and datasource ownership already exist
- datasource CRUD already exists
- there is already query validation and SQL-building direction

### Important Current Gaps

- runtime query execution still uses one global `query.datasource` in [DataSourceConfig.java](/C:/Users/ASUS/Desktop/ForgeQL/backend/src/main/java/com/example/DataSourceConfig.java#L45)
  This must be replaced by per-user, per-datasource runtime connections.

- schema is still static in [SchemaRegistry.java](/C:/Users/ASUS/Desktop/ForgeQL/backend/src/main/java/com/example/registry/SchemaRegistry.java#L18)
  The new core must generate schemas dynamically per datasource.

- current datasource request DTO uses `encryptedPassword` in [ReqDataSourceDTO.java](/C:/Users/ASUS/Desktop/ForgeQL/backend/src/main/java/com/example/controller/dtos/request/ReqDataSourceDTO.java#L15)
  but the service stores it directly in [DataSourceService.java](/C:/Users/ASUS/Desktop/ForgeQL/backend/src/main/java/com/example/service/DataSourceService.java#L66)
  This should be changed so the backend performs encryption itself.

- current `DatabaseTypes` still includes non-competition directions and does not matter yet, but the core build should implement PostgreSQL first and completely: [DatabaseTypes.java](/C:/Users/ASUS/Desktop/ForgeQL/backend/src/main/java/com/example/persistence/Enums/DatabaseTypes.java#L3)

- current SSL model is not sufficient for serious PostgreSQL coverage
  It should support the PostgreSQL JDBC SSL modes listed above.

- current runtime query endpoints are static-schema oriented
  They should be replaced by datasource-scoped core endpoints.

### Practical Conclusion

Do not extend the current static `schema.json` plus global `query.datasource` design.

Keep:

- auth
- user ownership checks
- general validation direction

Replace:

- static schema source
- static query datasource
- misleading password flow
- controller shape for schema and query

## 14. Recommended Package Structure

```text
com.example.core.postgres
com.example.core.postgres.api
com.example.core.postgres.api.controller
com.example.core.postgres.api.dto.request
com.example.core.postgres.api.dto.response
com.example.core.postgres.connection
com.example.core.postgres.introspection
com.example.core.postgres.schema
com.example.core.postgres.schema.model
com.example.core.postgres.schema.registry
com.example.core.postgres.schema.validation
com.example.core.postgres.ast
com.example.core.postgres.plan
com.example.core.postgres.sql
com.example.core.postgres.execution
com.example.core.postgres.aggregate
com.example.core.postgres.mutation
```

## 15. Implementation Order

1. Fix datasource model so it can represent a real PostgreSQL runtime connection.
2. Change password handling so encryption happens in backend service logic.
3. Remove dependency on static `query.datasource` for runtime exploration.
4. Build PostgreSQL connection factory and connection test service.
5. Build PostgreSQL metadata introspection.
6. Build generated schema model and registry.
7. Implement schema generation endpoints.
8. Implement table and column browsing endpoints.
9. Implement read AST, planner, SQL builder, and row exploration endpoint.
10. Implement aggregate AST, planner, SQL builder, and aggregate endpoint.
11. Implement insert, update, and delete AST, planners, SQL builders, and endpoints.
12. Add support for views and materialized views for read and aggregate paths.
13. Add PostgreSQL-specific type coverage:
    enum, uuid, json/jsonb, arrays, numeric, timestamptz.
14. Add connection coverage:
    SSL modes, timeouts, application name, extra JDBC options.
15. Add integration tests against real PostgreSQL instances and managed-like configs.
16. Add error mapping, metrics, and cache invalidation.

## 16. AST Strategy

The internal pipeline should be:

`request -> AST -> validated execution plan -> PostgreSQL SQL -> result mapping`

First stable AST set:

- `ReadTableAst`
- `ProjectionAst`
- `FilterAst`
- `PaginationAst`
- `SortAst`
- `AggregateAst`
- `InsertMutationAst`
- `UpdateMutationAst`
- `DeleteMutationAst`

Why this matters:

- validation stays centralized
- SQL generation stays deterministic
- aggregates do not become controller hacks
- mutations keep safety rules in planners
- later GraphQL-like features can extend the same AST and planning model

## 17. Final Direction

The April 26 competition version should be presented as:

- a **PostgreSQL-focused core engine**
- capable of connecting to **most normal PostgreSQL databases**
- able to **generate schema dynamically**
- able to **explore data**
- able to **aggregate data**
- able to **create, update, and delete records safely**

This is the right tradeoff for the competition:

- narrower than multi-database support
- much stronger and more credible as a real working backend engine
