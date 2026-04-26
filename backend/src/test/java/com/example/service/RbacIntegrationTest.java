package com.example.service;

import com.example.auth.InitialMainAdminInitializer;
import com.example.common.exceptions.ForbiddenException;
import com.example.controller.dtos.request.AssignDataSourceAccessDTO;
import com.example.controller.dtos.request.UpdateUserRoleDTO;
import com.example.persistence.Enums.DataSourceAccessRole;
import com.example.persistence.Enums.DataSourceConnectionStatus;
import com.example.persistence.Enums.DataSourceStatus;
import com.example.persistence.Enums.DatabaseTypes;
import com.example.persistence.Enums.GlobalRole;
import com.example.persistence.Enums.SslMode;
import com.example.persistence.model.DataSourceEntity;
import com.example.persistence.model.UserAccountEntity;
import com.example.persistence.repository.DataSourceAccessRepository;
import com.example.persistence.repository.DataSourceRepository;
import com.example.persistence.repository.JwtRepository;
import com.example.persistence.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class RbacIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("forgeql_rbac_test")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("app.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("app.datasource.username", POSTGRES::getUsername);
        registry.add("app.datasource.password", POSTGRES::getPassword);
        registry.add("app.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("datasource.encryption.secret", () -> "rbac-test-encryption-secret");
        registry.add("jwt.secret", () -> "rbac-test-jwt-secret-which-is-long-enough");
        registry.add("INITIAL_MAIN_ADMIN_EMAIL", () -> "owner@forgeql.test");
        registry.add("INITIAL_MAIN_ADMIN_PASSWORD", () -> "change-me-now");
        registry.add("INITIAL_MAIN_ADMIN_USERNAME", () -> "forgeql-owner");
    }

    @Autowired
    private AdminManagementService adminManagementService;

    @Autowired
    private DataSourceAuthorizationService dataSourceAuthorizationService;

    @Autowired
    private DataSourcePasswordCipher dataSourcePasswordCipher;

    @Autowired
    private DataSourceAccessRepository dataSourceAccessRepository;

    @Autowired
    private DataSourceRepository dataSourceRepository;

    @Autowired
    private JwtRepository jwtRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private InitialMainAdminInitializer initialMainAdminInitializer;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void resetState() {
        jwtRepository.deleteAll();
        dataSourceAccessRepository.deleteAll();
        dataSourceRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    @Test
    void mainAdminBootstrapCreatesConfiguredAccount() throws Exception {
        initialMainAdminInitializer.run(new DefaultApplicationArguments(new String[0]));

        UserAccountEntity mainAdmin = userAccountRepository.findByEmail("owner@forgeql.test")
                .orElseThrow(() -> new AssertionError("Expected bootstrapped MAIN_ADMIN"));

        assertThat(userAccountRepository.countByGlobalRole(GlobalRole.MAIN_ADMIN)).isEqualTo(1);
        assertThat(mainAdmin.getUsername()).isEqualTo("forgeql-owner");
        assertThat(mainAdmin.getGlobalRole()).isEqualTo(GlobalRole.MAIN_ADMIN);
        assertThat(passwordEncoder.matches("change-me-now", mainAdmin.getPasswordHash())).isTrue();
    }

    @Test
    void mainAdminCanDoEverything() {
        UserAccountEntity mainAdmin = createUser("main-admin", GlobalRole.MAIN_ADMIN);
        UserAccountEntity ownedAdmin = createUser("owned-admin", GlobalRole.ADMIN);
        UserAccountEntity targetMember = createUser("target-member", GlobalRole.MEMBER);
        UserAccountEntity targetViewer = createUser("target-viewer", GlobalRole.VIEWER);
        DataSourceEntity datasource = createDatasource(ownedAdmin, "main-admin-managed-ds");

        assertThatCode(() -> dataSourceAuthorizationService.assertCanCreateDatasource(mainAdmin))
                .doesNotThrowAnyException();
        assertThat(dataSourceAuthorizationService.getViewableDatasource(mainAdmin, datasource.getId()).getId())
                .isEqualTo(datasource.getId());
        assertThat(dataSourceAuthorizationService.getManageableDatasource(mainAdmin, datasource.getId()).getId())
                .isEqualTo(datasource.getId());
        assertThat(dataSourceAuthorizationService.getAdministrableDatasource(mainAdmin, datasource.getId()).getId())
                .isEqualTo(datasource.getId());

        adminManagementService.assignAccess(
                mainAdmin.getId(),
                datasource.getId(),
                new AssignDataSourceAccessDTO(targetViewer.getId(), DataSourceAccessRole.VIEWER)
        );
        adminManagementService.assignAccess(
                mainAdmin.getId(),
                datasource.getId(),
                new AssignDataSourceAccessDTO(targetMember.getId(), DataSourceAccessRole.MANAGER)
        );
        adminManagementService.updateUserRole(
                mainAdmin.getId(),
                ownedAdmin.getId(),
                new UpdateUserRoleDTO(GlobalRole.MEMBER)
        );

        assertThat(userAccountRepository.findById(ownedAdmin.getId()))
                .get()
                .extracting(UserAccountEntity::getGlobalRole)
                .isEqualTo(GlobalRole.MEMBER);
        assertThat(dataSourceAccessRepository.findByUser_IdAndDataSource_Id(targetViewer.getId(), datasource.getId()))
                .map(access -> access.getAccessRole())
                .contains(DataSourceAccessRole.VIEWER);
        assertThat(dataSourceAccessRepository.findByUser_IdAndDataSource_Id(targetMember.getId(), datasource.getId()))
                .map(access -> access.getAccessRole())
                .contains(DataSourceAccessRole.MANAGER);
    }

    @Test
    void adminCannotModifyMainAdmin() {
        UserAccountEntity mainAdmin = createUser("main-admin", GlobalRole.MAIN_ADMIN);
        UserAccountEntity admin = createUser("admin-user", GlobalRole.ADMIN);

        assertThatThrownBy(() -> adminManagementService.updateUserRole(
                admin.getId(),
                mainAdmin.getId(),
                new UpdateUserRoleDTO(GlobalRole.MEMBER)
        ))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("MAIN_ADMIN role cannot be removed");
    }

    @Test
    void adminCannotDemoteAnotherAdmin() {
        createUser("main-admin", GlobalRole.MAIN_ADMIN);
        UserAccountEntity actor = createUser("actor-admin", GlobalRole.ADMIN);
        UserAccountEntity target = createUser("target-admin", GlobalRole.ADMIN);

        assertThatThrownBy(() -> adminManagementService.updateUserRole(
                actor.getId(),
                target.getId(),
                new UpdateUserRoleDTO(GlobalRole.MEMBER)
        ))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Administrators cannot modify ADMIN or MAIN_ADMIN users");
    }

    @Test
    void adminCanPromoteMemberAndViewerToAdmin() {
        createUser("main-admin", GlobalRole.MAIN_ADMIN);
        UserAccountEntity actor = createUser("actor-admin", GlobalRole.ADMIN);
        UserAccountEntity member = createUser("member-user", GlobalRole.MEMBER);
        UserAccountEntity viewer = createUser("viewer-user", GlobalRole.VIEWER);

        adminManagementService.updateUserRole(
                actor.getId(),
                member.getId(),
                new UpdateUserRoleDTO(GlobalRole.ADMIN)
        );
        adminManagementService.updateUserRole(
                actor.getId(),
                viewer.getId(),
                new UpdateUserRoleDTO(GlobalRole.ADMIN)
        );

        assertThat(userAccountRepository.findById(member.getId()))
                .get()
                .extracting(UserAccountEntity::getGlobalRole)
                .isEqualTo(GlobalRole.ADMIN);
        assertThat(userAccountRepository.findById(viewer.getId()))
                .get()
                .extracting(UserAccountEntity::getGlobalRole)
                .isEqualTo(GlobalRole.ADMIN);
    }

    @Test
    void adminCanManageDatasourceAccessOnlyForOwnedDatasources() {
        createUser("main-admin", GlobalRole.MAIN_ADMIN);
        UserAccountEntity admin = createUser("owner-admin", GlobalRole.ADMIN);
        UserAccountEntity otherOwner = createUser("datasource-owner", GlobalRole.MEMBER);
        UserAccountEntity target = createUser("target-member", GlobalRole.MEMBER);
        DataSourceEntity ownedDatasource = createDatasource(admin, "owned-datasource");
        DataSourceEntity foreignDatasource = createDatasource(otherOwner, "foreign-datasource");

        adminManagementService.assignAccess(
                admin.getId(),
                ownedDatasource.getId(),
                new AssignDataSourceAccessDTO(target.getId(), DataSourceAccessRole.VIEWER)
        );

        assertThat(dataSourceAccessRepository.findByUser_IdAndDataSource_Id(target.getId(), ownedDatasource.getId()))
                .map(access -> access.getAccessRole())
                .contains(DataSourceAccessRole.VIEWER);

        assertThatThrownBy(() -> adminManagementService.assignAccess(
                admin.getId(),
                foreignDatasource.getId(),
                new AssignDataSourceAccessDTO(target.getId(), DataSourceAccessRole.VIEWER)
        ))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("You do not have permission to manage access for this datasource");
    }

    @Test
    void memberWithManagerAccessCanManageAssignedDatasource() {
        createUser("main-admin", GlobalRole.MAIN_ADMIN);
        UserAccountEntity owner = createUser("datasource-owner", GlobalRole.ADMIN);
        UserAccountEntity memberManager = createUser("member-manager", GlobalRole.MEMBER);
        UserAccountEntity target = createUser("target-viewer", GlobalRole.VIEWER);
        DataSourceEntity datasource = createDatasource(owner, "member-managed-datasource");
        createAccess(memberManager, datasource, DataSourceAccessRole.MANAGER);

        adminManagementService.assignAccess(
                memberManager.getId(),
                datasource.getId(),
                new AssignDataSourceAccessDTO(target.getId(), DataSourceAccessRole.VIEWER)
        );

        assertThat(dataSourceAccessRepository.findByUser_IdAndDataSource_Id(target.getId(), datasource.getId()))
                .map(access -> access.getAccessRole())
                .contains(DataSourceAccessRole.VIEWER);
    }

    @Test
    void viewerCanOnlyReadAssignedDatasource() {
        createUser("main-admin", GlobalRole.MAIN_ADMIN);
        UserAccountEntity owner = createUser("datasource-owner", GlobalRole.ADMIN);
        UserAccountEntity viewer = createUser("assigned-viewer", GlobalRole.VIEWER);
        DataSourceEntity datasource = createDatasource(owner, "viewer-assigned-datasource");
        createAccess(viewer, datasource, DataSourceAccessRole.VIEWER);

        assertThat(dataSourceAuthorizationService.getViewableDatasource(viewer, datasource.getId()).getId())
                .isEqualTo(datasource.getId());
        assertThat(dataSourceAuthorizationService.getAccess(viewer, datasource.getId()))
                .contains(DataSourceAccessRole.VIEWER);
        assertThatThrownBy(() -> dataSourceAuthorizationService.getManageableDatasource(viewer, datasource.getId()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("You do not have permission to manage this datasource");
        assertThatThrownBy(() -> adminManagementService.listAccess(viewer.getId(), datasource.getId()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("You do not have permission to manage access for this datasource");
    }

    @Test
    void unassignedUsersCannotAccessDatasource() {
        createUser("main-admin", GlobalRole.MAIN_ADMIN);
        UserAccountEntity owner = createUser("datasource-owner", GlobalRole.ADMIN);
        UserAccountEntity unassigned = createUser("unassigned-member", GlobalRole.MEMBER);
        DataSourceEntity datasource = createDatasource(owner, "private-datasource");

        assertThat(dataSourceAuthorizationService.canViewDatasource(unassigned, datasource.getId())).isFalse();
        assertThat(dataSourceAuthorizationService.getAccess(unassigned, datasource.getId()))
                .isEqualTo(Optional.empty());
        assertThatThrownBy(() -> dataSourceAuthorizationService.getViewableDatasource(unassigned, datasource.getId()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("You do not have permission to view this datasource");
        assertThatThrownBy(() -> dataSourceAuthorizationService.getManageableDatasource(unassigned, datasource.getId()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("You do not have permission to manage this datasource");
    }

    private UserAccountEntity createUser(String slug, GlobalRole globalRole) {
        UserAccountEntity user = new UserAccountEntity();
        user.setUsername(slug);
        user.setEmail(slug + "@forgeql.test");
        user.setPasswordHash(passwordEncoder.encode("password-" + slug));
        user.setGlobalRole(globalRole);
        return userAccountRepository.save(user);
    }

    private DataSourceEntity createDatasource(UserAccountEntity owner, String displayName) {
        DataSourceEntity datasource = new DataSourceEntity();
        datasource.setDisplayName(displayName);
        datasource.setDbType(DatabaseTypes.POSTGRESQL);
        datasource.setHost("db.internal.example");
        datasource.setPort(5432);
        datasource.setDatabaseName("forgeql_runtime");
        datasource.setSchemaName("public");
        datasource.setUsername("forgeql_app");
        datasource.setEncryptedPassword(dataSourcePasswordCipher.encrypt("secret-password"));
        datasource.setSslMode(SslMode.PREFER);
        datasource.setStatus(DataSourceStatus.ACTIVE);
        datasource.setLastConnectionStatus(DataSourceConnectionStatus.UNTESTED);
        datasource.setConnectTimeoutMs(5000);
        datasource.setSocketTimeoutMs(5000);
        datasource.setApplicationName("forgeql-rbac-test");
        datasource.setExtraJdbcOptionsJson(null);
        datasource.setUserAccount(owner);

        DataSourceEntity saved = dataSourceRepository.save(datasource);
        dataSourceAuthorizationService.ensureOwnerManagerAccess(saved);
        return saved;
    }

    private void createAccess(
            UserAccountEntity user,
            DataSourceEntity datasource,
            DataSourceAccessRole accessRole
    ) {
        adminManagementService.assignAccess(
                datasource.getUserAccount().getId(),
                datasource.getId(),
                new AssignDataSourceAccessDTO(user.getId(), accessRole)
        );
    }
}
