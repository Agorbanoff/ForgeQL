package com.example.core.postgres.connection;

import com.example.common.exceptions.InvalidDataSourceConfigurationException;
import com.example.common.exceptions.NoDataSourceFoundException;
import com.example.common.exceptions.UnsupportedDatabaseTypeException;
import com.example.persistence.Enums.SslMode;
import com.example.persistence.Enums.DatabaseTypes;
import com.example.persistence.model.DataSourceEntity;
import com.example.persistence.repository.DataSourceRepository;
import com.example.service.DataSourcePasswordCipher;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

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
        DataSourceEntity dataSourceEntity = dataSourceRepository.findById(datasourceId)
                .orElseThrow(() -> new NoDataSourceFoundException("Datasource not found"));

        return resolve(dataSourceEntity);
    }

    public PostgresRuntimeConnectionDefinition resolve(Integer datasourceId) {
        DataSourceEntity dataSourceEntity = dataSourceRepository.findById(datasourceId)
                .orElseThrow(() -> new NoDataSourceFoundException("Datasource not found"));

        return resolve(dataSourceEntity);
    }

    public PostgresRuntimeConnectionDefinition resolve(DataSourceEntity dataSourceEntity) {
        if (dataSourceEntity.getDbType() != DatabaseTypes.POSTGRESQL) {
            throw new UnsupportedDatabaseTypeException("Only PostgreSQL datasources are supported");
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
                resolveSslRootCertPath(dataSourceEntity.getSslRootCertRef(), dataSourceEntity.getSslMode()),
                dataSourceEntity.getExtraJdbcOptionsJson()
        );
    }

    private String resolveSslRootCertPath(String sslRootCertRef, SslMode sslMode) {
        if (sslRootCertRef == null || sslRootCertRef.isBlank()) {
            return null;
        }

        Path sslRootCertPath;
        try {
            sslRootCertPath = Path.of(sslRootCertRef.trim()).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            throw new InvalidDataSourceConfigurationException("sslRootCertRef must be a valid filesystem path");
        }

        if (requiresSslRootCert(sslMode)
                && (!Files.isRegularFile(sslRootCertPath) || !Files.isReadable(sslRootCertPath))) {
            throw new InvalidDataSourceConfigurationException(
                    "sslRootCertRef must point to a readable PostgreSQL root certificate file"
            );
        }

        return sslRootCertPath.toString();
    }

    private boolean requiresSslRootCert(SslMode sslMode) {
        return sslMode == SslMode.VERIFY_CA || sslMode == SslMode.VERIFY_FULL;
    }
}

