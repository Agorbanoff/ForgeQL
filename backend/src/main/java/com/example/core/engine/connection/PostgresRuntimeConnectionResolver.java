package com.example.core.engine.connection;

import com.example.common.CustomException;
import com.example.common.exceptions.NoDataSourceFoundException;
import com.example.persistence.Enums.DatabaseTypes;
import com.example.persistence.model.DataSourceEntity;
import com.example.persistence.repository.DataSourceRepository;
import com.example.service.DataSourcePasswordCipher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PostgresRuntimeConnectionResolver {

    private final DataSourceRepository dataSourceRepository;
    private final DataSourcePasswordCipher dataSourcePasswordCipher;

    public PostgresRuntimeConnectionResolver(
            DataSourceRepository dataSourceRepository,
            DataSourcePasswordCipher dataSourcePasswordCipher
    ) {
        this.dataSourceRepository = dataSourceRepository;
        this.dataSourcePasswordCipher = dataSourcePasswordCipher;
    }

    public PostgresRuntimeConnectionDefinition resolve(Integer datasourceId, Integer userId) {
        DataSourceEntity dataSourceEntity = dataSourceRepository.findByIdAndUserAccount_Id(datasourceId, userId)
                .orElseThrow(() -> new NoDataSourceFoundException("Datasource not found"));

        if (dataSourceEntity.getDbType() != DatabaseTypes.POSTGRESQL) {
            throw new CustomException("Only PostgreSQL datasources are supported", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        return new PostgresRuntimeConnectionDefinition(
                dataSourceEntity.getId(),
                dataSourceEntity.getUserAccount().getId(),
                dataSourceEntity.getDisplayName(),
                dataSourceEntity.getHost(),
                dataSourceEntity.getPort(),
                dataSourceEntity.getDatabaseName(),
                dataSourceEntity.getSchemaName(),
                dataSourceEntity.getUsername(),
                dataSourcePasswordCipher.decrypt(dataSourceEntity.getEncryptedPassword()),
                dataSourceEntity.getSslMode(),
                dataSourceEntity.getConnectTimeoutMs(),
                dataSourceEntity.getSocketTimeoutMs(),
                dataSourceEntity.getApplicationName(),
                dataSourceEntity.getSslRootCertRef(),
                dataSourceEntity.getExtraJdbcOptionsJson()
        );
    }
}
