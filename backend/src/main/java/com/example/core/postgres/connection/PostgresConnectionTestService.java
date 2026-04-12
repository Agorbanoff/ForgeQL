package com.example.core.postgres.connection;

import com.example.common.CustomException;
import com.example.common.PostgresJdbcErrorClassifier;
import com.example.common.PostgresJdbcErrorClassifier.ErrorCategory;
import com.example.common.exceptions.NoDataSourceFoundException;
import com.example.common.exceptions.PostgresAuthenticationFailedException;
import com.example.common.exceptions.PostgresConnectionFailedException;
import com.example.common.exceptions.PostgresConnectionTimeoutException;
import com.example.common.exceptions.PostgresSslFailureException;
import com.example.persistence.Enums.DataSourceConnectionStatus;
import com.example.persistence.Enums.DataSourceStatus;
import com.example.persistence.model.DataSourceEntity;
import com.example.persistence.repository.DataSourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.time.Instant;

@Service
public class PostgresConnectionTestService {

    private final PostgresRuntimeConnectionResolver runtimeConnectionResolver;
    private final PostgresConnectionFactory connectionFactory;
    private final PostgresConnectionValidator connectionValidator;
    private final DataSourceRepository dataSourceRepository;
    private final PostgresJdbcErrorClassifier postgresJdbcErrorClassifier;

    public PostgresConnectionTestService(
            PostgresRuntimeConnectionResolver runtimeConnectionResolver,
            PostgresConnectionFactory connectionFactory,
            PostgresConnectionValidator connectionValidator,
            DataSourceRepository dataSourceRepository,
            PostgresJdbcErrorClassifier postgresJdbcErrorClassifier
    ) {
        this.runtimeConnectionResolver = runtimeConnectionResolver;
        this.connectionFactory = connectionFactory;
        this.connectionValidator = connectionValidator;
        this.dataSourceRepository = dataSourceRepository;
        this.postgresJdbcErrorClassifier = postgresJdbcErrorClassifier;
    }

    @Transactional
    public PostgresConnectionTestResult test(Integer datasourceId, Integer userId) {
        DataSourceEntity dataSourceEntity = dataSourceRepository.findByIdAndUserAccount_Id(datasourceId, userId)
                .orElseThrow(() -> new NoDataSourceFoundException("Datasource not found"));

        PostgresRuntimeConnectionDefinition definition = runtimeConnectionResolver.resolve(dataSourceEntity);
        Instant testedAt = Instant.now();

        try (Connection connection = connectionFactory.openConnection(definition)) {
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            connectionValidator.validateProductName(databaseMetaData.getDatabaseProductName());

            String databaseProductName = databaseMetaData.getDatabaseProductName();
            String serverVersion = databaseMetaData.getDatabaseProductVersion();

            markSuccess(dataSourceEntity, testedAt, serverVersion);

            return new PostgresConnectionTestResult(
                    datasourceId,
                    true,
                    testedAt,
                    DataSourceConnectionStatus.SUCCEEDED,
                    databaseProductName,
                    serverVersion,
                    "PostgreSQL connection test succeeded"
            );
        } catch (CustomException e) {
            markFailure(dataSourceEntity, testedAt, DataSourceConnectionStatus.FAILED, e.getMessage());
            throw e;
        } catch (SQLException e) {
            DataSourceConnectionStatus connectionStatus = resolveConnectionStatus(e);
            String safeMessage = resolveSafeMessage(e, connectionStatus);
            markFailure(dataSourceEntity, testedAt, connectionStatus, safeMessage);
            throw buildConnectionException(e, connectionStatus);
        }
    }

    private void markSuccess(DataSourceEntity dataSourceEntity, Instant testedAt, String serverVersion) {
        dataSourceEntity.setStatus(DataSourceStatus.ACTIVE);
        dataSourceEntity.setLastConnectionTestAt(testedAt);
        dataSourceEntity.setLastConnectionStatus(DataSourceConnectionStatus.SUCCEEDED);
        dataSourceEntity.setLastConnectionError(null);
        dataSourceEntity.setServerVersion(serverVersion);
        dataSourceRepository.save(dataSourceEntity);
    }

    private void markFailure(
            DataSourceEntity dataSourceEntity,
            Instant testedAt,
            DataSourceConnectionStatus connectionStatus,
            String safeMessage
    ) {

        dataSourceEntity.setLastConnectionTestAt(testedAt);
        dataSourceEntity.setLastConnectionStatus(connectionStatus);
        dataSourceEntity.setLastConnectionError(safeMessage);
        dataSourceEntity.setServerVersion(null);
        dataSourceRepository.save(dataSourceEntity);
    }

    private DataSourceConnectionStatus resolveConnectionStatus(SQLException exception) {
        if (postgresJdbcErrorClassifier.classify(exception) == ErrorCategory.TIMEOUT) {
            return DataSourceConnectionStatus.TIMED_OUT;
        }

        return DataSourceConnectionStatus.FAILED;
    }

    private CustomException buildConnectionException(
            SQLException exception,
            DataSourceConnectionStatus connectionStatus
    ) {
        ErrorCategory errorCategory = postgresJdbcErrorClassifier.classify(exception);
        if (connectionStatus == DataSourceConnectionStatus.TIMED_OUT) {
            return new PostgresConnectionTimeoutException("Connection to PostgreSQL timed out");
        }
        if (errorCategory == ErrorCategory.SSL_FAILURE) {
            return new PostgresSslFailureException("SSL negotiation with PostgreSQL failed");
        }
        if (errorCategory == ErrorCategory.AUTHENTICATION_FAILURE) {
            return new PostgresAuthenticationFailedException("Authentication with PostgreSQL failed");
        }
        return new PostgresConnectionFailedException("Connection to PostgreSQL failed");
    }

    private String resolveSafeMessage(SQLException exception, DataSourceConnectionStatus connectionStatus) {
        ErrorCategory errorCategory = postgresJdbcErrorClassifier.classify(exception);
        if (connectionStatus == DataSourceConnectionStatus.TIMED_OUT) {
            return "Connection to PostgreSQL timed out";
        }
        if (errorCategory == ErrorCategory.SSL_FAILURE) {
            return "SSL negotiation with PostgreSQL failed";
        }
        if (errorCategory == ErrorCategory.AUTHENTICATION_FAILURE) {
            return "Authentication with PostgreSQL failed";
        }
        return "Connection to PostgreSQL failed";
    }
}
