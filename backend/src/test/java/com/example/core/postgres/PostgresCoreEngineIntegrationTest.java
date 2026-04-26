package com.example.core.postgres;

import com.example.auth.filter.AuthenticatedUser;
import com.example.common.ApplicationErrorCode;
import com.example.common.exceptions.PostgresTargetMismatchException;
import com.example.core.postgres.aggregate.AggregateFunction;
import com.example.core.postgres.api.dto.request.AggregateRequest;
import com.example.core.postgres.api.dto.request.AggregateSelectionRequest;
import com.example.core.postgres.api.dto.request.CreateRowRequest;
import com.example.core.postgres.api.dto.request.ReadFieldFilterRequest;
import com.example.core.postgres.api.dto.request.ReadRowsRequest;
import com.example.core.postgres.api.dto.request.ReadSortDirection;
import com.example.core.postgres.api.dto.request.ReadSortRequest;
import com.example.core.postgres.api.dto.request.UpdateRowRequest;
import com.example.core.postgres.api.dto.response.AggregateResponse;
import com.example.core.postgres.api.dto.response.CreateRowResponse;
import com.example.core.postgres.api.dto.response.DeleteRowResponse;
import com.example.core.postgres.api.dto.response.RowsResponse;
import com.example.core.postgres.api.dto.response.UpdateRowResponse;
import com.example.core.postgres.connection.PostgresConnectionTestResult;
import com.example.core.postgres.connection.PostgresConnectionTestService;
import com.example.core.postgres.connection.PostgresConnectionValidator;
import com.example.core.postgres.connection.PostgresRuntimePoolManager;
import com.example.core.postgres.execution.AggregateQueryService;
import com.example.core.postgres.execution.MutationRowsService;
import com.example.core.postgres.execution.ReadRowsService;
import com.example.core.postgres.schema.SchemaGenerationService;
import com.example.core.postgres.schema.SchemaReadService;
import com.example.core.postgres.schema.model.GeneratedSchema;
import com.example.core.postgres.schema.model.SchemaColumn;
import com.example.core.postgres.schema.model.SchemaRelation;
import com.example.core.postgres.schema.model.SchemaTable;
import com.example.core.postgres.schema.model.SchemaTableType;
import com.example.persistence.Enums.DataSourceConnectionStatus;
import com.example.persistence.Enums.DataSourceStatus;
import com.example.persistence.Enums.DatabaseTypes;
import com.example.persistence.Enums.SslMode;
import com.example.persistence.model.DataSourceEntity;
import com.example.persistence.model.UserAccountEntity;
import com.example.persistence.repository.DataSourceAccessRepository;
import com.example.persistence.repository.DataSourceRepository;
import com.example.persistence.repository.JwtRepository;
import com.example.persistence.repository.UserAccountRepository;
import com.example.service.DataSourceAuthorizationService;
import com.example.service.DataSourcePasswordCipher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class PostgresCoreEngineIntegrationTest {

    private static final String RUNTIME_SCHEMA = "runtime_suite";

    @Container
    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("forgeql_core_test")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("app.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("app.datasource.username", POSTGRES::getUsername);
        registry.add("app.datasource.password", POSTGRES::getPassword);
        registry.add("app.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("datasource.encryption.secret", () -> "integration-test-encryption-secret");
        registry.add("jwt.secret", () -> "integration-test-jwt-secret-which-is-long-enough");
        registry.add("spring.jpa.show-sql", () -> "false");
        registry.add("core.postgres.pool.max-active-pools", () -> "4");
        registry.add("core.postgres.pool.max-size", () -> "2");
        registry.add("core.postgres.pool.min-idle", () -> "0");
        registry.add("core.postgres.pool.idle-eviction-ms", () -> "10000");
        registry.add("core.postgres.pool.cleanup-interval-ms", () -> "10000");
        registry.add("core.postgres.pool.borrow-timeout-ms", () -> "5000");
    }

    @Autowired
    private DataSourceRepository dataSourceRepository;

    @Autowired
    private DataSourceAccessRepository dataSourceAccessRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private JwtRepository jwtRepository;

    @Autowired
    private DataSourcePasswordCipher dataSourcePasswordCipher;

    @Autowired
    private DataSourceAuthorizationService dataSourceAuthorizationService;

    @Autowired
    private PostgresConnectionTestService postgresConnectionTestService;

    @Autowired
    private PostgresConnectionValidator postgresConnectionValidator;

    @Autowired
    private SchemaGenerationService schemaGenerationService;

    @Autowired
    private SchemaReadService schemaReadService;

    @Autowired
    private ReadRowsService readRowsService;

    @Autowired
    private AggregateQueryService aggregateQueryService;

    @Autowired
    private MutationRowsService mutationRowsService;

    @Autowired
    private PostgresRuntimePoolManager runtimePoolManager;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void resetState() throws SQLException {
        runtimePoolManager.shutdown();
        jwtRepository.deleteAll();
        dataSourceAccessRepository.deleteAll();
        dataSourceRepository.deleteAll();
        userAccountRepository.deleteAll();
        rebuildRuntimeSchema();
    }

    @Test
    void datasourceConnectionUsesTheConfiguredRuntimeDatasource() {
        TestFixture fixture = createRuntimeDatasourceFixture(POSTGRES.getPassword(), RUNTIME_SCHEMA);

        PostgresConnectionTestResult result = postgresConnectionTestService.test(
                fixture.datasourceId(),
                fixture.userId()
        );

        DataSourceEntity datasource = findDatasource(fixture.datasourceId());
        assertThat(result.successful()).isTrue();
        assertThat(result.datasourceId()).isEqualTo(fixture.datasourceId());
        assertThat(result.connectionStatus()).isEqualTo(DataSourceConnectionStatus.SUCCEEDED);
        assertThat(result.databaseProductName()).isEqualTo("PostgreSQL");
        assertThat(result.serverVersion()).isNotBlank();
        assertThat(datasource.getStatus()).isEqualTo(DataSourceStatus.ACTIVE);
        assertThat(datasource.getLastConnectionStatus()).isEqualTo(DataSourceConnectionStatus.SUCCEEDED);
        assertThat(datasource.getServerVersion()).isNotBlank();
        assertThat(datasource.getLastConnectionError()).isNull();
    }

    @Test
    void productValidationRejectsNonPostgresTargets() {
        assertThatThrownBy(() -> postgresConnectionValidator.validateProductName("CockroachDB"))
                .isInstanceOf(PostgresTargetMismatchException.class)
                .hasMessage("Target datasource is not PostgreSQL");
    }

    @Test
    void schemaGenerationDiscoversOnlyRuntimeUsersAndOrders() {
        TestFixture fixture = createRuntimeDatasourceFixture(POSTGRES.getPassword(), RUNTIME_SCHEMA);

        GeneratedSchema schema = generateRuntimeSchema(fixture);
        List<SchemaTable> tables = schemaReadService.getTables(fixture.datasourceId(), fixture.userId());

        assertThat(schema.defaultSchema()).isEqualTo(RUNTIME_SCHEMA);
        assertThat(schema.fingerprint()).isNotBlank();
        assertThat(tables).extracting(SchemaTable::qualifiedName)
                .contains("runtime_suite.orders", "runtime_suite.users");

        SchemaTable users = schema.tables().get("runtime_suite.users");
        SchemaTable orders = schema.tables().get("runtime_suite.orders");

        assertThat(users.primaryKey().columns()).containsExactly("id");
        assertThat(users.uniqueConstraints())
                .anySatisfy(constraint -> assertThat(constraint.columns()).containsExactly("email"));
        assertThat(orders.foreignKeys())
                .anySatisfy(foreignKey -> {
                    assertThat(foreignKey.sourceQualifiedName()).isEqualTo("runtime_suite.orders");
                    assertThat(foreignKey.targetQualifiedName()).isEqualTo("runtime_suite.users");
                    assertThat(foreignKey.sourceColumns()).containsExactly("user_id");
                    assertThat(foreignKey.targetColumns()).containsExactly("id");
                });
        assertThat(users.tableType()).isEqualTo(SchemaTableType.TABLE);
        assertThat(orders.tableType()).isEqualTo(SchemaTableType.TABLE);
    }

    @Test
    void schemaReadHidesViewsFromTheExplorerTableList() throws SQLException {
        executeRuntimeStatements(
                """
                CREATE VIEW runtime_suite.customer_order_totals AS
                SELECT
                    u.id AS customer_id,
                    u.email,
                    u.name AS full_name,
                    COUNT(o.id) AS total_orders,
                    COALESCE(SUM(o.amount), 0) AS total_amount
                FROM runtime_suite.users u
                LEFT JOIN runtime_suite.orders o ON o.user_id = u.id
                GROUP BY u.id, u.email, u.name
                """
        );

        TestFixture fixture = createRuntimeDatasourceFixture(POSTGRES.getPassword(), RUNTIME_SCHEMA);

        GeneratedSchema schema = generateRuntimeSchema(fixture);
        List<SchemaTable> explorerTables = schemaReadService.getTables(fixture.datasourceId(), fixture.userId());

        assertThat(schema.tables()).containsKey("runtime_suite.customer_order_totals");
        assertThat(explorerTables)
                .extracting(SchemaTable::qualifiedName)
                .contains("runtime_suite.orders", "runtime_suite.users")
                .doesNotContain("runtime_suite.customer_order_totals");
    }

    @Test
    void schemaReadExposesOrderColumnsRelationsAndMutationCapabilities() {
        TestFixture fixture = createRuntimeDatasourceFixture(POSTGRES.getPassword(), RUNTIME_SCHEMA);
        generateRuntimeSchema(fixture);

        SchemaTable orders = schemaReadService.getTable(fixture.datasourceId(), fixture.userId(), "runtime_suite.orders");
        List<SchemaColumn> columns = schemaReadService.getTableColumns(
                fixture.datasourceId(),
                fixture.userId(),
                "runtime_suite.orders"
        );
        List<SchemaRelation> relations = schemaReadService.getTableRelations(
                fixture.datasourceId(),
                fixture.userId(),
                "runtime_suite.orders"
        );

        assertThat(orders.capabilities().read()).isTrue();
        assertThat(orders.capabilities().aggregate()).isTrue();
        assertThat(orders.capabilities().insert()).isTrue();
        assertThat(orders.capabilities().update()).isTrue();
        assertThat(orders.capabilities().delete()).isTrue();

        assertThat(columns)
                .extracting(SchemaColumn::name)
                .containsExactly("id", "user_id", "status", "amount", "note", "tags", "created_at", "updated_at");

        SchemaColumn status = findColumn(columns, "status");
        SchemaColumn amount = findColumn(columns, "amount");
        SchemaColumn tags = findColumn(columns, "tags");
        SchemaColumn createdAt = findColumn(columns, "created_at");

        assertThat(status.enumType()).isTrue();
        assertThat(status.enumLabels()).containsExactly("pending", "paid", "cancelled");
        assertThat(amount.numericType()).isTrue();
        assertThat(amount.precision()).isEqualTo(12);
        assertThat(amount.scale()).isEqualTo(2);
        assertThat(tags.arrayType()).isTrue();
        assertThat(tags.arrayElementTypeName()).isEqualTo("text");
        assertThat(createdAt.timestampWithTimeZone()).isTrue();
        assertThat(createdAt.defaultValue()).containsIgnoringCase("now()");

        assertThat(relations)
                .anySatisfy(relation -> {
                    assertThat(relation.sourceQualifiedName()).isEqualTo("runtime_suite.orders");
                    assertThat(relation.targetQualifiedName()).isEqualTo("runtime_suite.users");
                    assertThat(relation.sourceColumns()).containsExactly("user_id");
                    assertThat(relation.targetColumns()).containsExactly("id");
                });
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("readFilterScenarios")
    void readRowsSupportsFilterOperatorsAgainstRuntimeOrders(
            String description,
            Map<String, ReadFieldFilterRequest> filter,
            List<Long> expectedIds
    ) {
        TestFixture fixture = createRuntimeDatasourceFixture(POSTGRES.getPassword(), RUNTIME_SCHEMA);
        generateRuntimeSchema(fixture);

        RowsResponse response = readRowsService.readRows(
                fixture.datasourceId(),
                fixture.userId(),
                "runtime_suite.orders",
                new ReadRowsRequest(
                        List.of("id"),
                        filter,
                        List.of(new ReadSortRequest("id", ReadSortDirection.ASC)),
                        20,
                        0
                )
        );

        assertThat(response.page().returnedCount()).isEqualTo(expectedIds.size());
        assertThat(response.rows())
                .extracting(row -> toLong(row.get("id")))
                .containsExactlyElementsOf(expectedIds);
    }

    @Test
    void readRowsSupportsProjectionSortingAndPagination() {
        TestFixture fixture = createRuntimeDatasourceFixture(POSTGRES.getPassword(), RUNTIME_SCHEMA);
        generateRuntimeSchema(fixture);

        RowsResponse response = readRowsService.readRows(
                fixture.datasourceId(),
                fixture.userId(),
                "runtime_suite.orders",
                new ReadRowsRequest(
                        List.of("id", "status", "amount"),
                        null,
                        List.of(new ReadSortRequest("amount", ReadSortDirection.DESC)),
                        2,
                        1
                )
        );

        assertThat(response.page().returnedCount()).isEqualTo(2);
        assertThat(response.page().limit()).isEqualTo(2);
        assertThat(response.page().offset()).isEqualTo(1);
        assertThat(response.rows())
                .allSatisfy(row -> assertThat(row).containsOnlyKeys("id", "status", "amount"));
        assertThat(response.rows())
                .extracting(row -> toLong(row.get("id")))
                .containsExactly(2L, 1L);
    }

    @Test
    void aggregateQueriesReturnGroupedRollupsForRuntimeOrders() {
        TestFixture fixture = createRuntimeDatasourceFixture(POSTGRES.getPassword(), RUNTIME_SCHEMA);
        generateRuntimeSchema(fixture);

        AggregateResponse response = aggregateQueryService.aggregate(
                fixture.datasourceId(),
                fixture.userId(),
                "runtime_suite.orders",
                new AggregateRequest(
                        List.of(
                                new AggregateSelectionRequest(AggregateFunction.COUNT, null, "row_count"),
                                new AggregateSelectionRequest(AggregateFunction.SUM, "amount", "total_amount")
                        ),
                        List.of("status"),
                        Map.of("user_id", inFilter(1, 3))
                )
        );

        Map<String, Map<String, Object>> grouped = response.rows().stream()
                .map(aggregateRow -> aggregateRow.values())
                .collect(java.util.stream.Collectors.toMap(
                        row -> row.get("status").toString(),
                        row -> row
                ));

        assertThat(grouped.keySet()).containsExactlyInAnyOrder("paid", "pending");
        assertThat(toLong(grouped.get("pending").get("row_count"))).isEqualTo(1L);
        assertThat(decimalValue(grouped.get("pending").get("total_amount"))).isEqualByComparingTo("99.99");
        assertThat(toLong(grouped.get("paid").get("row_count"))).isEqualTo(2L);
        assertThat(decimalValue(grouped.get("paid").get("total_amount"))).isEqualByComparingTo("284.60");
    }

    @Test
    void insertUpdateAndDeleteUseTheRuntimeMutationPath() {
        TestFixture fixture = createRuntimeDatasourceFixture(POSTGRES.getPassword(), RUNTIME_SCHEMA);
        generateRuntimeSchema(fixture);

        CreateRowResponse created = mutationRowsService.createRow(
                fixture.datasourceId(),
                fixture.userId(),
                "runtime_suite.orders",
                new CreateRowRequest(Map.of(
                        "user_id", 2,
                        "status", "pending",
                        "amount", "45.25",
                        "note", "created in integration test",
                        "tags", List.of("new", "qa")
                ))
        );

        Long createdId = toLong(created.createdIdentity());
        assertThat(created.affectedRows()).isEqualTo(1L);
        assertThat(createdId).isNotNull();
        assertThat(created.row()).containsEntry("status", "pending");

        UpdateRowResponse updated = mutationRowsService.updateRow(
                fixture.datasourceId(),
                fixture.userId(),
                "runtime_suite.orders",
                createdId,
                new UpdateRowRequest(Map.of(
                        "status", "paid",
                        "amount", "50.00",
                        "note", "updated in integration test"
                ))
        );

        assertThat(updated.affectedRows()).isEqualTo(1L);
        assertThat(updated.row()).containsEntry("status", "paid");
        assertThat(decimalValue(updated.row().get("amount"))).isEqualByComparingTo("50.00");

        DeleteRowResponse deleted = mutationRowsService.deleteRow(
                fixture.datasourceId(),
                fixture.userId(),
                "runtime_suite.orders",
                createdId
        );

        RowsResponse afterDelete = readRowsService.readRows(
                fixture.datasourceId(),
                fixture.userId(),
                "runtime_suite.orders",
                new ReadRowsRequest(
                        List.of("id"),
                        Map.of("id", eqFilter(createdId)),
                        List.of(new ReadSortRequest("id", ReadSortDirection.ASC)),
                        10,
                        0
                )
        );

        assertThat(deleted.affectedRows()).isEqualTo(1L);
        assertThat(toLong(deleted.deletedIdentity())).isEqualTo(createdId);
        assertThat(afterDelete.rows()).isEmpty();
    }

    @Test
    void uniqueConstraintViolationsMapToConflict() throws Exception {
        TestFixture fixture = createRuntimeDatasourceFixture(POSTGRES.getPassword(), RUNTIME_SCHEMA);
        generateRuntimeSchema(fixture);

        mockMvc.perform(post("/core/datasources/{datasourceId}/tables/{tableName}/rows", fixture.datasourceId(), "runtime_suite.users")
                        .with(authenticated(fixture))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new CreateRowRequest(Map.of(
                                "email", "alice@example.com",
                                "name", "Alice Again"
                        )))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ApplicationErrorCode.UNIQUE_CONSTRAINT_VIOLATION.name()))
                .andExpect(jsonPath("$.message").value("Unique constraint violated"))
                .andExpect(jsonPath("$.datasourceId").value(fixture.datasourceId()));
    }

    @Test
    void foreignKeyViolationsMapToConflict() throws Exception {
        TestFixture fixture = createRuntimeDatasourceFixture(POSTGRES.getPassword(), RUNTIME_SCHEMA);
        generateRuntimeSchema(fixture);

        mockMvc.perform(post("/core/datasources/{datasourceId}/tables/{tableName}/rows", fixture.datasourceId(), "runtime_suite.orders")
                        .with(authenticated(fixture))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new CreateRowRequest(Map.of(
                                "user_id", 9999,
                                "status", "pending",
                                "amount", "25.00"
                        )))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ApplicationErrorCode.FOREIGN_KEY_VIOLATION.name()))
                .andExpect(jsonPath("$.message").value("Foreign key constraint violated"))
                .andExpect(jsonPath("$.datasourceId").value(fixture.datasourceId()));
    }

    @Test
    void databaseValidationFailuresMapToBadRequest() throws Exception {
        TestFixture fixture = createRuntimeDatasourceFixture(POSTGRES.getPassword(), RUNTIME_SCHEMA);
        generateRuntimeSchema(fixture);

        mockMvc.perform(post("/core/datasources/{datasourceId}/tables/{tableName}/rows", fixture.datasourceId(), "runtime_suite.orders")
                        .with(authenticated(fixture))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new CreateRowRequest(Map.of(
                                "user_id", 1,
                                "status", "pending",
                                "amount", "-5.00"
                        )))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApplicationErrorCode.POSTGRES_VALIDATION_FAILURE.name()))
                .andExpect(jsonPath("$.message").value("PostgreSQL rejected the request values"))
                .andExpect(jsonPath("$.datasourceId").value(fixture.datasourceId()));
    }

    @Test
    void datasourceAuthenticationFailuresMapThroughTheConnectionApi() throws Exception {
        TestFixture fixture = createRuntimeDatasourceFixture("wrong-password", RUNTIME_SCHEMA);

        mockMvc.perform(post("/datasource/{id}/test-connection", fixture.datasourceId())
                        .with(authenticated(fixture))
                        .with(csrf()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value(ApplicationErrorCode.POSTGRES_AUTHENTICATION_FAILURE.name()))
                .andExpect(jsonPath("$.message").value("Authentication with PostgreSQL failed"));
    }

    private GeneratedSchema generateRuntimeSchema(TestFixture fixture) {
        return schemaGenerationService.generate(fixture.datasourceId(), fixture.userId());
    }

    private TestFixture createRuntimeDatasourceFixture(String runtimePassword, String schemaName) {
        UserAccountEntity user = new UserAccountEntity();
        user.setUsername("runtime-owner");
        user.setEmail("runtime-owner@example.com");
        user.setPasswordHash("hashed-password");
        UserAccountEntity savedUser = userAccountRepository.save(user);

        DataSourceEntity datasource = new DataSourceEntity();
        datasource.setDisplayName("Integration runtime datasource");
        datasource.setDbType(DatabaseTypes.POSTGRESQL);
        datasource.setHost(POSTGRES.getHost());
        datasource.setPort(POSTGRES.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT));
        datasource.setDatabaseName(POSTGRES.getDatabaseName());
        datasource.setUsername(POSTGRES.getUsername());
        datasource.setEncryptedPassword(dataSourcePasswordCipher.encrypt(runtimePassword));
        datasource.setSchemaName(schemaName);
        datasource.setSslMode(SslMode.DISABLE);
        datasource.setStatus(DataSourceStatus.INACTIVE);
        datasource.setLastConnectionStatus(DataSourceConnectionStatus.UNTESTED);
        datasource.setConnectTimeoutMs(1000);
        datasource.setSocketTimeoutMs(1000);
        datasource.setApplicationName("forgeql-core-engine-it");
        datasource.setExtraJdbcOptionsJson(null);
        datasource.setUserAccount(savedUser);

        DataSourceEntity savedDatasource = dataSourceRepository.save(datasource);
        dataSourceAuthorizationService.ensureOwnerManagerAccess(savedDatasource);
        return new TestFixture(savedUser.getId(), savedUser.getEmail(), savedDatasource.getId());
    }

    private RequestPostProcessor authenticated(TestFixture fixture) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(fixture.userId(), fixture.email()),
                null,
                AuthorityUtils.NO_AUTHORITIES
        );
        return SecurityMockMvcRequestPostProcessors.authentication(authentication);
    }

    private DataSourceEntity findDatasource(Integer datasourceId) {
        return dataSourceRepository.findById(datasourceId)
                .orElseThrow(() -> new AssertionError("Datasource was not persisted"));
    }

    private SchemaColumn findColumn(List<SchemaColumn> columns, String columnName) {
        return columns.stream()
                .filter(column -> column.name().equals(columnName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Column not found: " + columnName));
    }

    private void rebuildRuntimeSchema() throws SQLException {
        executeRuntimeStatements(
                "DROP SCHEMA IF EXISTS " + RUNTIME_SCHEMA + " CASCADE",
                "CREATE SCHEMA " + RUNTIME_SCHEMA,
                "CREATE TYPE " + RUNTIME_SCHEMA + ".order_status AS ENUM ('pending', 'paid', 'cancelled')",
                """
                CREATE TABLE runtime_suite.users (
                    id BIGSERIAL PRIMARY KEY,
                    email TEXT NOT NULL UNIQUE,
                    name TEXT NOT NULL
                )
                """,
                """
                CREATE TABLE runtime_suite.orders (
                    id BIGSERIAL PRIMARY KEY,
                    user_id BIGINT NOT NULL REFERENCES runtime_suite.users(id),
                    status runtime_suite.order_status NOT NULL,
                    amount NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
                    note TEXT,
                    tags TEXT[],
                    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                    updated_at TIMESTAMP
                )
                """,
                """
                INSERT INTO runtime_suite.users (id, email, name) VALUES
                    (1, 'alice@example.com', 'Alice'),
                    (2, 'bob@example.com', 'Bob'),
                    (3, 'carol@example.com', 'Carol')
                """,
                """
                INSERT INTO runtime_suite.orders (id, user_id, status, amount, note, tags, created_at, updated_at) VALUES
                    (1, 1, 'pending', 99.99, 'first order', ARRAY['fragile'], '2026-04-10T10:00:00Z', '2026-04-10T10:00:00'),
                    (2, 1, 'paid', 120.50, 'priority shipment', ARRAY['priority', 'gift'], '2026-04-10T11:00:00Z', '2026-04-10T11:00:00'),
                    (3, 2, 'cancelled', 80.00, NULL, ARRAY['return'], '2026-04-11T08:30:00Z', '2026-04-11T08:30:00'),
                    (4, 3, 'paid', 164.10, 'gift order', NULL, '2026-04-11T09:45:00Z', '2026-04-11T09:45:00')
                """,
                "SELECT setval(pg_get_serial_sequence('runtime_suite.users', 'id'), 3, true)",
                "SELECT setval(pg_get_serial_sequence('runtime_suite.orders', 'id'), 4, true)"
        );
    }

    private void executeRuntimeStatements(String... statements) throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        ); Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
        }
    }

    private static Stream<Arguments> readFilterScenarios() {
        return Stream.of(
                Arguments.of(
                        "eq filter on enum status",
                        Map.of("status", eqFilter("paid")),
                        List.of(2L, 4L)
                ),
                Arguments.of(
                        "gt filter on numeric amount",
                        Map.of("amount", gtFilter("100")),
                        List.of(2L, 4L)
                ),
                Arguments.of(
                        "between filter on numeric amount",
                        Map.of("amount", betweenFilter("90", "130")),
                        List.of(1L, 2L)
                ),
                Arguments.of(
                        "in filter on foreign key user_id",
                        Map.of("user_id", inFilter(1, 3)),
                        List.of(1L, 2L, 4L)
                ),
                Arguments.of(
                        "ilike filter on note",
                        Map.of("note", ilikeFilter("%PRIORITY%")),
                        List.of(2L)
                ),
                Arguments.of(
                        "is null filter on note",
                        Map.of("note", isNullFilter()),
                        List.of(3L)
                )
        );
    }

    private static ReadFieldFilterRequest eqFilter(Object value) {
        return new ReadFieldFilterRequest(value, null, null, null, null, null, null, null, null, null, null, null);
    }

    private static ReadFieldFilterRequest gtFilter(Object value) {
        return new ReadFieldFilterRequest(null, null, value, null, null, null, null, null, null, null, null, null);
    }

    private static ReadFieldFilterRequest betweenFilter(Object start, Object end) {
        return new ReadFieldFilterRequest(null, null, null, null, null, null, null, List.of(start, end), null, null, null, null);
    }

    private static ReadFieldFilterRequest inFilter(Object... values) {
        return new ReadFieldFilterRequest(null, null, null, null, null, null, List.of(values), null, null, null, null, null);
    }

    private static ReadFieldFilterRequest ilikeFilter(String value) {
        return new ReadFieldFilterRequest(null, null, null, null, null, null, null, null, null, value, null, null);
    }

    private static ReadFieldFilterRequest isNullFilter() {
        return new ReadFieldFilterRequest(null, null, null, null, null, null, null, null, null, null, true, null);
    }

    private static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(value.toString());
    }

    private static BigDecimal decimalValue(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(value.toString());
    }

    private record TestFixture(Integer userId, String email, Integer datasourceId) {
    }
}
