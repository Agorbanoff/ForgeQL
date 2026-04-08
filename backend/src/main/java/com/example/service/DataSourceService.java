package com.example.service;


import com.example.common.exceptions.DataSourceAlreadyExistsException;
import com.example.common.exceptions.MissingRequiredFieldException;
import com.example.common.exceptions.NoDataSourceFoundException;
import com.example.common.exceptions.UnsupportedDatabaseTypeException;
import com.example.common.exceptions.UserNotFoundException;
import com.example.controller.dtos.request.ReqDataSourceDTO;
import com.example.controller.dtos.request.UpdateDataSourceDTO;
import com.example.controller.dtos.response.ResDataSourceDTO;
import com.example.persistence.Enums.DataSourceConnectionStatus;
import com.example.persistence.Enums.DataSourceStatus;
import com.example.persistence.Enums.DatabaseTypes;
import com.example.persistence.model.DataSourceEntity;
import com.example.persistence.model.UserAccountEntity;
import com.example.persistence.repository.DataSourceRepository;
import com.example.persistence.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class DataSourceService {
    private record NormalizedConnectionDefinition(
            String displayName,
            DatabaseTypes dbType,
            String host,
            Integer port,
            String databaseName,
            String schemaName,
            String username,
            String password,
            boolean passwordProvided,
            com.example.persistence.Enums.SslMode sslMode,
            Integer connectTimeoutMs,
            Integer socketTimeoutMs,
            String applicationName,
            String sslRootCertRef,
            String extraJdbcOptionsJson
    ) {
    }

    private final DataSourceRepository dataSourceRepository;
    private final UserAccountRepository userAccountRepository;
    private final DataSourcePasswordCipher dataSourcePasswordCipher;

    @Autowired
    public DataSourceService(DataSourceRepository dataSourceRepository,
                             UserAccountRepository userAccountRepository,
                             DataSourcePasswordCipher dataSourcePasswordCipher) {
        this.dataSourceRepository = dataSourceRepository;
        this.userAccountRepository = userAccountRepository;
        this.dataSourcePasswordCipher = dataSourcePasswordCipher;
    }

    private ResDataSourceDTO mapToDTO(DataSourceEntity dataSourceEntity) {
        ResDataSourceDTO resDataSourceDTO = new ResDataSourceDTO();
        resDataSourceDTO.setId(dataSourceEntity.getId());
        resDataSourceDTO.setOwnerUserId(dataSourceEntity.getUserAccount().getId());
        resDataSourceDTO.setDisplayName(dataSourceEntity.getDisplayName());
        resDataSourceDTO.setDbType(dataSourceEntity.getDbType());
        resDataSourceDTO.setHost(dataSourceEntity.getHost());
        resDataSourceDTO.setPort(dataSourceEntity.getPort());
        resDataSourceDTO.setDatabaseName(dataSourceEntity.getDatabaseName());
        resDataSourceDTO.setSchemaName(dataSourceEntity.getSchemaName());
        resDataSourceDTO.setUsername(dataSourceEntity.getUsername());
        resDataSourceDTO.setSslMode(dataSourceEntity.getSslMode());
        resDataSourceDTO.setConnectTimeoutMs(dataSourceEntity.getConnectTimeoutMs());
        resDataSourceDTO.setSocketTimeoutMs(dataSourceEntity.getSocketTimeoutMs());
        resDataSourceDTO.setApplicationName(dataSourceEntity.getApplicationName());
        resDataSourceDTO.setSslRootCertRef(dataSourceEntity.getSslRootCertRef());
        resDataSourceDTO.setExtraJdbcOptionsJson(dataSourceEntity.getExtraJdbcOptionsJson());
        resDataSourceDTO.setStatus(dataSourceEntity.getStatus());
        resDataSourceDTO.setLastConnectionTestAt(dataSourceEntity.getLastConnectionTestAt());
        resDataSourceDTO.setLastConnectionStatus(dataSourceEntity.getLastConnectionStatus());
        resDataSourceDTO.setLastConnectionError(dataSourceEntity.getLastConnectionError());
        resDataSourceDTO.setLastSchemaGeneratedAt(dataSourceEntity.getLastSchemaGeneratedAt());
        resDataSourceDTO.setLastSchemaFingerprint(dataSourceEntity.getLastSchemaFingerprint());
        resDataSourceDTO.setServerVersion(dataSourceEntity.getServerVersion());
        resDataSourceDTO.setCreatedAt(dataSourceEntity.getCreatedAt());
        resDataSourceDTO.setUpdatedAt(dataSourceEntity.getUpdatedAt());

        return resDataSourceDTO;
    }

    private void validatePostgreSqlOnly(DatabaseTypes dbType) {
        if (dbType != DatabaseTypes.POSTGRESQL) {
            throw new UnsupportedDatabaseTypeException("Only PostgreSQL datasources are supported");
        }
    }

    private String normalizeRequiredValue(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new MissingRequiredFieldException(fieldName + " is required");
        }

        return value.trim();
    }

    private String normalizeOptionalValue(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeOptionalPassword(String password) {
        if (password == null) {
            return null;
        }

        String normalized = password.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private NormalizedConnectionDefinition normalizeCreateRequest(ReqDataSourceDTO reqDataSourceDTO) {
        validatePostgreSqlOnly(reqDataSourceDTO.getDbType());

        return new NormalizedConnectionDefinition(
                normalizeRequiredValue(reqDataSourceDTO.getDisplayName(), "displayName"),
                reqDataSourceDTO.getDbType(),
                normalizeRequiredValue(reqDataSourceDTO.getHost(), "host"),
                reqDataSourceDTO.getPort(),
                normalizeRequiredValue(reqDataSourceDTO.getDatabaseName(), "databaseName"),
                normalizeRequiredValue(reqDataSourceDTO.getSchemaName(), "schemaName"),
                normalizeRequiredValue(reqDataSourceDTO.getUsername(), "username"),
                normalizeRequiredValue(reqDataSourceDTO.getPassword(), "password"),
                true,
                reqDataSourceDTO.getSslMode(),
                reqDataSourceDTO.getConnectTimeoutMs(),
                reqDataSourceDTO.getSocketTimeoutMs(),
                normalizeOptionalValue(reqDataSourceDTO.getApplicationName()),
                normalizeOptionalValue(reqDataSourceDTO.getSslRootCertRef()),
                normalizeOptionalValue(reqDataSourceDTO.getExtraJdbcOptionsJson())
        );
    }

    private NormalizedConnectionDefinition normalizeUpdateRequest(UpdateDataSourceDTO updateDataSourceDTO) {
        validatePostgreSqlOnly(updateDataSourceDTO.getDbType());

        String normalizedPassword = normalizeOptionalPassword(updateDataSourceDTO.getPassword());

        return new NormalizedConnectionDefinition(
                normalizeRequiredValue(updateDataSourceDTO.getDisplayName(), "displayName"),
                updateDataSourceDTO.getDbType(),
                normalizeRequiredValue(updateDataSourceDTO.getHost(), "host"),
                updateDataSourceDTO.getPort(),
                normalizeRequiredValue(updateDataSourceDTO.getDatabaseName(), "databaseName"),
                normalizeRequiredValue(updateDataSourceDTO.getSchemaName(), "schemaName"),
                normalizeRequiredValue(updateDataSourceDTO.getUsername(), "username"),
                normalizedPassword,
                normalizedPassword != null,
                updateDataSourceDTO.getSslMode(),
                updateDataSourceDTO.getConnectTimeoutMs(),
                updateDataSourceDTO.getSocketTimeoutMs(),
                normalizeOptionalValue(updateDataSourceDTO.getApplicationName()),
                normalizeOptionalValue(updateDataSourceDTO.getSslRootCertRef()),
                normalizeOptionalValue(updateDataSourceDTO.getExtraJdbcOptionsJson())
        );
    }

    private void applyConnectionDefinition(
            DataSourceEntity dataSourceEntity,
            NormalizedConnectionDefinition connectionDefinition
    ) {
        dataSourceEntity.setDisplayName(connectionDefinition.displayName());
        dataSourceEntity.setDbType(connectionDefinition.dbType());
        dataSourceEntity.setHost(connectionDefinition.host());
        dataSourceEntity.setPort(connectionDefinition.port());
        dataSourceEntity.setDatabaseName(connectionDefinition.databaseName());
        dataSourceEntity.setSchemaName(connectionDefinition.schemaName());
        dataSourceEntity.setUsername(connectionDefinition.username());
        dataSourceEntity.setSslMode(connectionDefinition.sslMode());
        dataSourceEntity.setConnectTimeoutMs(connectionDefinition.connectTimeoutMs());
        dataSourceEntity.setSocketTimeoutMs(connectionDefinition.socketTimeoutMs());
        dataSourceEntity.setApplicationName(connectionDefinition.applicationName());
        dataSourceEntity.setSslRootCertRef(connectionDefinition.sslRootCertRef());
        dataSourceEntity.setExtraJdbcOptionsJson(connectionDefinition.extraJdbcOptionsJson());

        if (shouldUpdatePassword(dataSourceEntity, connectionDefinition)) {
            dataSourceEntity.setEncryptedPassword(dataSourcePasswordCipher.encrypt(connectionDefinition.password()));
        }
    }

    private boolean shouldUpdatePassword(
            DataSourceEntity dataSourceEntity,
            NormalizedConnectionDefinition connectionDefinition
    ) {
        if (!connectionDefinition.passwordProvided()) {
            return false;
        }

        if (dataSourceEntity.getEncryptedPassword() == null || dataSourceEntity.getEncryptedPassword().isBlank()) {
            return true;
        }

        String existingPassword = dataSourcePasswordCipher.decrypt(dataSourceEntity.getEncryptedPassword());
        return !Objects.equals(existingPassword, connectionDefinition.password());
    }

    private boolean hasConnectionDefinitionChanged(
            DataSourceEntity dataSourceEntity,
            NormalizedConnectionDefinition connectionDefinition
    ) {
        if (!Objects.equals(dataSourceEntity.getDbType(), connectionDefinition.dbType())) return true;
        if (!Objects.equals(dataSourceEntity.getHost(), connectionDefinition.host())) return true;
        if (!Objects.equals(dataSourceEntity.getPort(), connectionDefinition.port())) return true;
        if (!Objects.equals(dataSourceEntity.getDatabaseName(), connectionDefinition.databaseName())) return true;
        if (!Objects.equals(dataSourceEntity.getSchemaName(), connectionDefinition.schemaName())) return true;
        if (!Objects.equals(dataSourceEntity.getUsername(), connectionDefinition.username())) return true;
        if (!Objects.equals(dataSourceEntity.getSslMode(), connectionDefinition.sslMode())) return true;
        if (!Objects.equals(dataSourceEntity.getConnectTimeoutMs(), connectionDefinition.connectTimeoutMs())) return true;
        if (!Objects.equals(dataSourceEntity.getSocketTimeoutMs(), connectionDefinition.socketTimeoutMs())) return true;
        if (!Objects.equals(dataSourceEntity.getApplicationName(), connectionDefinition.applicationName())) return true;
        if (!Objects.equals(dataSourceEntity.getSslRootCertRef(), connectionDefinition.sslRootCertRef())) return true;
        if (!Objects.equals(dataSourceEntity.getExtraJdbcOptionsJson(), connectionDefinition.extraJdbcOptionsJson())) return true;
        return shouldUpdatePassword(dataSourceEntity, connectionDefinition);
    }

    private void resetConnectionMetadata(DataSourceEntity dataSourceEntity) {
        dataSourceEntity.setLastConnectionTestAt(null);
        dataSourceEntity.setLastConnectionStatus(DataSourceConnectionStatus.UNTESTED);
        dataSourceEntity.setLastConnectionError(null);
        dataSourceEntity.setLastSchemaGeneratedAt(null);
        dataSourceEntity.setLastSchemaFingerprint(null);
        dataSourceEntity.setServerVersion(null);
    }

    public ResDataSourceDTO getDataSource(Integer id, Integer userId) {
        DataSourceEntity dataSourceEntity = dataSourceRepository.findByIdAndUserAccount_Id(id, userId)
                            .orElseThrow(() -> new NoDataSourceFoundException("Datasource not found"));

        return mapToDTO(dataSourceEntity);
    }

    public List<ResDataSourceDTO> getAllDataSource(Integer userId) {
        List<DataSourceEntity> dataSourceEntities = dataSourceRepository.findAllByUserAccount_Id(userId);

        List<ResDataSourceDTO> resDataSourceDTOList = new ArrayList<>();
        for (DataSourceEntity dataSourceEntity : dataSourceEntities) {
            resDataSourceDTOList.add(mapToDTO(dataSourceEntity));
        }

        return resDataSourceDTOList;
    }

    public void saveDataSource(ReqDataSourceDTO reqDataSourceDTO, Integer userId) {
        UserAccountEntity userAccountEntity = userAccountRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        NormalizedConnectionDefinition connectionDefinition = normalizeCreateRequest(reqDataSourceDTO);

        if (dataSourceRepository.existsByUserAccount_IdAndDbTypeAndHostAndPortAndDatabaseNameAndSchemaNameAndUsername(
                userId,
                connectionDefinition.dbType(),
                connectionDefinition.host(),
                connectionDefinition.port(),
                connectionDefinition.databaseName(),
                connectionDefinition.schemaName(),
                connectionDefinition.username()
        )) {
            throw new DataSourceAlreadyExistsException("Datasource already exists");
        }

        DataSourceEntity dataSourceEntity = new DataSourceEntity();
        applyConnectionDefinition(dataSourceEntity, connectionDefinition);
        dataSourceEntity.setStatus(DataSourceStatus.ACTIVE);
        resetConnectionMetadata(dataSourceEntity);
        dataSourceEntity.setUserAccount(userAccountEntity);

        dataSourceRepository.save(dataSourceEntity);
    }

    public void deleteDataSource(Integer id, Integer userId) {
        DataSourceEntity dataSourceEntity = dataSourceRepository.findByIdAndUserAccount_Id(id, userId)
                .orElseThrow(() -> new NoDataSourceFoundException("Datasource not found"));

        dataSourceRepository.delete(dataSourceEntity);
    }

    public void updateDataSource(UpdateDataSourceDTO updateDataSourceDTO, Integer userId, Integer id) {
        DataSourceEntity dataSourceEntity = dataSourceRepository.findByIdAndUserAccount_Id(id, userId)
                .orElseThrow(() -> new NoDataSourceFoundException("Datasource not found"));

        NormalizedConnectionDefinition connectionDefinition = normalizeUpdateRequest(updateDataSourceDTO);

        if (dataSourceRepository.existsByUserAccount_IdAndDbTypeAndHostAndPortAndDatabaseNameAndSchemaNameAndUsernameAndIdNot(
                userId,
                connectionDefinition.dbType(),
                connectionDefinition.host(),
                connectionDefinition.port(),
                connectionDefinition.databaseName(),
                connectionDefinition.schemaName(),
                connectionDefinition.username(),
                id
        )) {
            throw new DataSourceAlreadyExistsException("Datasource already exists");
        }

        boolean connectionDefinitionChanged = hasConnectionDefinitionChanged(dataSourceEntity, connectionDefinition);

        applyConnectionDefinition(dataSourceEntity, connectionDefinition);
        if (dataSourceEntity.getStatus() == null) {
            dataSourceEntity.setStatus(DataSourceStatus.ACTIVE);
        }
        if (connectionDefinitionChanged) {
            resetConnectionMetadata(dataSourceEntity);
        }

        dataSourceRepository.save(dataSourceEntity);
    }
}
