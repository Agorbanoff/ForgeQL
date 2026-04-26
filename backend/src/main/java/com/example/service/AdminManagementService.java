package com.example.service;

import com.example.common.exceptions.DataSourceAccessAlreadyExistsException;
import com.example.common.exceptions.DataSourceAccessNotFoundException;
import com.example.common.exceptions.ForbiddenException;
import com.example.common.exceptions.NoDataSourceFoundException;
import com.example.common.exceptions.UserNotFoundException;
import com.example.controller.dtos.request.AssignDataSourceAccessDTO;
import com.example.controller.dtos.request.UpdateDataSourceAccessDTO;
import com.example.controller.dtos.request.UpdateUserRoleDTO;
import com.example.controller.dtos.response.AdminUserDTO;
import com.example.controller.dtos.response.DataSourceAccessDTO;
import com.example.persistence.Enums.DataSourceAccessRole;
import com.example.persistence.Enums.GlobalRole;
import com.example.persistence.model.DataSourceAccessEntity;
import com.example.persistence.model.DataSourceEntity;
import com.example.persistence.model.UserAccountEntity;
import com.example.persistence.repository.DataSourceAccessRepository;
import com.example.persistence.repository.DataSourceRepository;
import com.example.persistence.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminManagementService {

    private final DataSourceAuthorizationService dataSourceAuthorizationService;
    private final DataSourceRepository dataSourceRepository;
    private final DataSourceAccessRepository dataSourceAccessRepository;
    private final UserAccountRepository userAccountRepository;

    public AdminManagementService(
            DataSourceAuthorizationService dataSourceAuthorizationService,
            DataSourceRepository dataSourceRepository,
            DataSourceAccessRepository dataSourceAccessRepository,
            UserAccountRepository userAccountRepository
    ) {
        this.dataSourceAuthorizationService = dataSourceAuthorizationService;
        this.dataSourceRepository = dataSourceRepository;
        this.dataSourceAccessRepository = dataSourceAccessRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional
    public void assignAccess(Integer adminUserId, Integer datasourceId, AssignDataSourceAccessDTO request) {
        UserAccountEntity actor = requireAdminOrMainAdmin(adminUserId);
        DataSourceEntity dataSource = getRequiredDatasource(datasourceId);
        validateDatasourceAccessManagement(actor, dataSource);
        UserAccountEntity targetUser = getRequiredUser(request.userId());
        validateAssignableAccess(targetUser, dataSource, request.accessRole());

        if (dataSourceAccessRepository.existsByUser_IdAndDataSource_Id(targetUser.getId(), dataSource.getId())) {
            throw new DataSourceAccessAlreadyExistsException("Datasource access already exists for this user");
        }

        DataSourceAccessEntity accessEntity = new DataSourceAccessEntity();
        accessEntity.setDataSource(dataSource);
        accessEntity.setUser(targetUser);
        accessEntity.setAccessRole(request.accessRole());

        dataSourceAccessRepository.save(accessEntity);
    }

    @Transactional
    public void updateAccess(
            Integer adminUserId,
            Integer datasourceId,
            Integer targetUserId,
            UpdateDataSourceAccessDTO request
    ) {
        UserAccountEntity actor = requireAdminOrMainAdmin(adminUserId);
        DataSourceEntity dataSource = getRequiredDatasource(datasourceId);
        validateDatasourceAccessManagement(actor, dataSource);
        UserAccountEntity targetUser = getRequiredUser(targetUserId);
        validateAssignableAccess(targetUser, dataSource, request.accessRole());

        DataSourceAccessEntity accessEntity = dataSourceAccessRepository.findByUser_IdAndDataSource_Id(targetUserId, datasourceId)
                .orElseThrow(() -> new DataSourceAccessNotFoundException("Datasource access not found"));

        accessEntity.setAccessRole(request.accessRole());
        dataSourceAccessRepository.save(accessEntity);
    }

    @Transactional
    public void removeAccess(Integer adminUserId, Integer datasourceId, Integer targetUserId) {
        UserAccountEntity actor = requireAdminOrMainAdmin(adminUserId);
        DataSourceEntity dataSource = getRequiredDatasource(datasourceId);
        validateDatasourceAccessManagement(actor, dataSource);
        getRequiredUser(targetUserId);
        if (isDatasourceOwner(targetUserId, dataSource)) {
            throw new ForbiddenException("Datasource owner access cannot be removed");
        }

        DataSourceAccessEntity accessEntity = dataSourceAccessRepository.findByUser_IdAndDataSource_Id(targetUserId, datasourceId)
                .orElseThrow(() -> new DataSourceAccessNotFoundException("Datasource access not found"));

        dataSourceAccessRepository.delete(accessEntity);
    }

    @Transactional(readOnly = true)
    public List<DataSourceAccessDTO> listAccess(Integer adminUserId, Integer datasourceId) {
        UserAccountEntity actor = requireAdminOrMainAdmin(adminUserId);
        DataSourceEntity dataSource = getRequiredDatasource(datasourceId);
        validateDatasourceAccessManagement(actor, dataSource);

        return dataSourceAccessRepository.findAllByDataSource_IdOrderByCreatedAtAsc(datasourceId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminUserDTO> listUsers(Integer adminUserId) {
        requireAdminOrMainAdmin(adminUserId);

        return userAccountRepository.findAllByOrderByUsernameAsc()
                .stream()
                .map(user -> new AdminUserDTO(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getGlobalRole(),
                        user.getCreatedAt()
                ))
                .toList();
    }

    @Transactional
    public void updateUserRole(Integer adminUserId, Integer targetUserId, UpdateUserRoleDTO request) {
        UserAccountEntity actor = requireAdminOrMainAdmin(adminUserId);
        UserAccountEntity targetUser = getRequiredUser(targetUserId);
        validateRoleChange(actor, targetUser, request.globalRole());

        targetUser.setGlobalRole(request.globalRole());
        userAccountRepository.save(targetUser);
    }

    private DataSourceAccessDTO mapToDto(DataSourceAccessEntity accessEntity) {
        UserAccountEntity user = accessEntity.getUser();
        return new DataSourceAccessDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                accessEntity.getAccessRole(),
                accessEntity.getCreatedAt()
        );
    }

    private UserAccountEntity requireAdminOrMainAdmin(Integer adminUserId) {
        UserAccountEntity adminUser = dataSourceAuthorizationService.getRequiredUser(adminUserId);
        if (adminUser.getGlobalRole() != GlobalRole.MAIN_ADMIN
                && adminUser.getGlobalRole() != GlobalRole.ADMIN) {
            throw new ForbiddenException("Only administrators can perform this operation");
        }

        return adminUser;
    }

    private void validateRoleChange(UserAccountEntity actor, UserAccountEntity targetUser, GlobalRole newRole) {
        GlobalRole actorRole = actor.getGlobalRole();
        GlobalRole targetRole = targetUser.getGlobalRole();
        long mainAdminCount = userAccountRepository.countByGlobalRole(GlobalRole.MAIN_ADMIN);

        if (mainAdminCount != 1) {
            throw new ForbiddenException("System must contain exactly one MAIN_ADMIN");
        }

        if (targetRole == GlobalRole.MAIN_ADMIN && newRole != GlobalRole.MAIN_ADMIN) {
            throw new ForbiddenException("MAIN_ADMIN role cannot be removed");
        }

        if (newRole == GlobalRole.MAIN_ADMIN && targetRole != GlobalRole.MAIN_ADMIN) {
            throw new ForbiddenException("Only one MAIN_ADMIN is allowed in the system");
        }

        if (actorRole != GlobalRole.MAIN_ADMIN) {
            if (actor.getId().equals(targetUser.getId())) {
                throw new ForbiddenException("Administrators cannot modify their own role");
            }
            if (targetRole == GlobalRole.ADMIN || targetRole == GlobalRole.MAIN_ADMIN) {
                throw new ForbiddenException("Administrators cannot modify ADMIN or MAIN_ADMIN users");
            }
        }
    }

    private UserAccountEntity getRequiredUser(Integer userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    private void validateAssignableAccess(
            UserAccountEntity targetUser,
            DataSourceEntity dataSource,
            DataSourceAccessRole accessRole
    ) {
        if (targetUser.getGlobalRole() == GlobalRole.ADMIN
                || targetUser.getGlobalRole() == GlobalRole.MAIN_ADMIN) {
            throw new ForbiddenException("Datasource access cannot be assigned to administrators");
        }
        if (targetUser.getGlobalRole() == GlobalRole.VIEWER && accessRole == DataSourceAccessRole.MANAGER) {
            throw new ForbiddenException("Global viewers cannot receive datasource manager access");
        }
        if (isDatasourceOwner(targetUser.getId(), dataSource) && accessRole != DataSourceAccessRole.MANAGER) {
            throw new ForbiddenException("Datasource owner access must remain MANAGER");
        }
    }

    private boolean isDatasourceOwner(Integer userId, DataSourceEntity dataSource) {
        return dataSource.getUserAccount().getId().equals(userId);
    }

    private void validateDatasourceAccessManagement(UserAccountEntity actor, DataSourceEntity dataSource) {
        if (actor.getGlobalRole() == GlobalRole.MAIN_ADMIN) {
            return;
        }
        if (actor.getGlobalRole() == GlobalRole.ADMIN && isDatasourceOwner(actor.getId(), dataSource)) {
            return;
        }

        throw new ForbiddenException("You do not have permission to manage access for this datasource");
    }

    private DataSourceEntity getRequiredDatasource(Integer datasourceId) {
        return dataSourceRepository.findById(datasourceId)
                .orElseThrow(() -> new NoDataSourceFoundException("Datasource not found"));
    }
}
