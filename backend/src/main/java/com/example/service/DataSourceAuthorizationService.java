package com.example.service;

import com.example.common.exceptions.ForbiddenException;
import com.example.common.exceptions.NoDataSourceFoundException;
import com.example.common.exceptions.UserNotFoundException;
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

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class DataSourceAuthorizationService {

    private static final EnumSet<DataSourceAccessRole> VIEW_ACCESS_ROLES =
            EnumSet.of(DataSourceAccessRole.MANAGER, DataSourceAccessRole.VIEWER);

    private final DataSourceAccessRepository dataSourceAccessRepository;
    private final DataSourceRepository dataSourceRepository;
    private final UserAccountRepository userAccountRepository;

    public DataSourceAuthorizationService(
            DataSourceAccessRepository dataSourceAccessRepository,
            DataSourceRepository dataSourceRepository,
            UserAccountRepository userAccountRepository
    ) {
        this.dataSourceAccessRepository = dataSourceAccessRepository;
        this.dataSourceRepository = dataSourceRepository;
        this.userAccountRepository = userAccountRepository;
    }

    public UserAccountEntity getRequiredUser(Integer userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    public void assertCanCreateDatasource(Integer userId) {
        assertCanCreateDatasource(getRequiredUser(userId));
    }

    public void assertCanCreateDatasource(UserAccountEntity user) {
        if (user == null || user.getGlobalRole() == GlobalRole.VIEWER) {
            throw new ForbiddenException("You do not have permission to create datasources");
        }
    }

    public DataSourceEntity getViewableDatasource(Integer userId, Integer datasourceId) {
        return getViewableDatasource(getRequiredUser(userId), datasourceId);
    }

    public DataSourceEntity getViewableDatasource(UserAccountEntity user, Integer datasourceId) {
        DataSourceEntity dataSource = getExistingDatasource(datasourceId);
        if (canViewDatasource(user, datasourceId)) {
            return dataSource;
        }

        throw new ForbiddenException("You do not have permission to view this datasource");
    }

    public DataSourceEntity getManageableDatasource(Integer userId, Integer datasourceId) {
        return getManageableDatasource(getRequiredUser(userId), datasourceId);
    }

    public DataSourceEntity getManageableDatasource(UserAccountEntity user, Integer datasourceId) {
        DataSourceEntity dataSource = getExistingDatasource(datasourceId);
        if (canManageDatasource(user, datasourceId)) {
            return dataSource;
        }

        throw new ForbiddenException("You do not have permission to manage this datasource");
    }

    public List<DataSourceEntity> getReadableDatasources(Integer userId) {
        return getReadableDatasources(getRequiredUser(userId));
    }

    public List<DataSourceEntity> getReadableDatasources(UserAccountEntity user) {
        if (isAdmin(user)) {
            return dataSourceRepository.findAllByOrderByDisplayNameAsc();
        }

        return dataSourceRepository.findDistinctByDataSourceAccesses_User_IdOrderByDisplayNameAsc(user.getId());
    }

    public boolean isAdmin(UserAccountEntity user) {
        return user != null
                && (user.getGlobalRole() == GlobalRole.ADMIN
                || user.getGlobalRole() == GlobalRole.MAIN_ADMIN);
    }

    public boolean canViewDatasource(UserAccountEntity user, Integer datasourceId) {
        if (user == null || datasourceId == null) {
            return false;
        }

        if (isAdmin(user)) {
            return true;
        }

        return hasDatasourceAccess(user, datasourceId, VIEW_ACCESS_ROLES);
    }

    public boolean canManageDatasource(UserAccountEntity user, Integer datasourceId) {
        if (user == null || datasourceId == null) {
            return false;
        }

        if (isAdmin(user)) {
            return true;
        }

        if (user.getGlobalRole() != GlobalRole.MEMBER) {
            return false;
        }

        return hasDatasourceAccess(user, datasourceId, EnumSet.of(DataSourceAccessRole.MANAGER));
    }

    public Optional<DataSourceAccessRole> getAccess(UserAccountEntity user, Integer datasourceId) {
        if (user == null || datasourceId == null) {
            return Optional.empty();
        }

        if (isAdmin(user)) {
            return Optional.of(DataSourceAccessRole.MANAGER);
        }

        return dataSourceAccessRepository.findByUser_IdAndDataSource_Id(user.getId(), datasourceId)
                .map(DataSourceAccessEntity::getAccessRole);
    }

    @Transactional
    public void ensureOwnerManagerAccess(DataSourceEntity dataSourceEntity) {
        Integer ownerUserId = dataSourceEntity.getUserAccount().getId();
        Integer datasourceId = dataSourceEntity.getId();

        DataSourceAccessEntity accessEntity = dataSourceAccessRepository
                .findByUser_IdAndDataSource_Id(ownerUserId, datasourceId)
                .orElseGet(() -> {
                    DataSourceAccessEntity createdAccess = new DataSourceAccessEntity();
                    createdAccess.setUser(dataSourceEntity.getUserAccount());
                    createdAccess.setDataSource(dataSourceEntity);
                    return createdAccess;
                });

        accessEntity.setAccessRole(DataSourceAccessRole.MANAGER);
        dataSourceAccessRepository.save(accessEntity);
    }

    private boolean hasDatasourceAccess(
            UserAccountEntity user,
            Integer datasourceId,
            EnumSet<DataSourceAccessRole> accessRoles
    ) {
        if (user == null || datasourceId == null) {
            return false;
        }

        return dataSourceAccessRepository.existsByUser_IdAndDataSource_IdAndAccessRoleIn(
                user.getId(),
                datasourceId,
                accessRoles
        );
    }

    private DataSourceEntity getExistingDatasource(Integer datasourceId) {
        return dataSourceRepository.findById(datasourceId)
                .orElseThrow(() -> new NoDataSourceFoundException("Datasource not found"));
    }
}
