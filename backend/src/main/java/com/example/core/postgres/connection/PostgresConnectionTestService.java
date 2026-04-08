package com.example.core.postgres.connection;

import com.example.common.CustomException;
import com.example.common.exceptions.NoDataSourceFoundException;
import com.example.common.exceptions.PostgresAuthenticationFailedException;
import com.example.common.exceptions.PostgresConnectionFailedException;
import com.example.common.exceptions.PostgresConnectionTimeoutException;
import com.example.persistence.Enums.DataSourceConnectionStatus;
import com.example.persistence.Enums.DataSourceStatus;
import com.example.persistence.model.DataSourceEntity;
import com.example.persistence.repository.DataSourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.net.ssl.SSLException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.SQLInvalidAuthorizationSpecException;
import java.sql.SQLTimeoutException;
import java.time.Instant;
import java.util.Locale;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;

@Service
public class PostgresConnectionTestService {

    private final PostgresRuntimeConnectionResolver runtimeConnectionResolver;
    private final PostgresConnectionFactory connectionFactory;
    private final PostgresConnectionValidator connectionValidator;
    private final DataSourceRepository dataSourceRepository;

    public PostgresConnectionTestService(
            PostgresRuntimeConnectionResolver runtimeConnectionResolver,
            PostgresConnectionFactory connectionFactory,
            PostgresConnectionValidator connectionValidator,
            DataSourceRepository dataSourceRepository
    ) {
        this.runtimeConnectionResolver = runtimeConnectionResolver;
        this.connectionFactory = connectionFactory;
        this.connectionValidator = connectionValidator;
        this.dataSourceRepository = dataSourceRepository;
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
        if (exception instanceof SQLTimeoutException || containsTimeoutCause(exception)) {
            return DataSourceConnectionStatus.TIMED_OUT;
        }

        return DataSourceConnectionStatus.FAILED;
    }

    private CustomException buildConnectionException(
            SQLException exception,
            DataSourceConnectionStatus connectionStatus
    ) {
        if (connectionStatus == DataSourceConnectionStatus.TIMED_OUT) {
            return new PostgresConnectionTimeoutException("Connection to PostgreSQL timed out");
        }
        if (isAuthenticationOrSslFailure(exception)) {
            return new PostgresAuthenticationFailedException(
                    "Authentication or SSL negotiation with PostgreSQL failed"
            );
        }
        return new PostgresConnectionFailedException("Connection to PostgreSQL failed");
    }

    private String resolveSafeMessage(SQLException exception, DataSourceConnectionStatus connectionStatus) {
        if (connectionStatus == DataSourceConnectionStatus.TIMED_OUT) {
            return "Connection to PostgreSQL timed out";
        }
        if (isAuthenticationOrSslFailure(exception)) {
            return "Authentication or SSL negotiation with PostgreSQL failed";
        }
        return "Connection to PostgreSQL failed";
    }

    private boolean isAuthenticationOrSslFailure(SQLException exception) {
        return isAuthenticationFailure(exception) || isSslFailure(exception);
    }

    private boolean isAuthenticationFailure(SQLException exception) {
        if (exception instanceof SQLInvalidAuthorizationSpecException) {
            return true;
        }

        return hasSqlState(exception, "28");
    }

    private boolean isSslFailure(SQLException exception) {
        if (containsCause(exception, SSLException.class)
                || containsCause(exception, CertificateException.class)
                || containsCause(exception, CertPathValidatorException.class)) {
            return true;
        }

        String message = exception.getMessage();
        return message != null && message.toLowerCase(Locale.ROOT).contains("ssl");
    }

    private boolean hasSqlState(SQLException exception, String sqlStatePrefix) {
        SQLException current = exception;
        while (current != null) {
            String sqlState = current.getSQLState();
            if (sqlState != null && sqlState.startsWith(sqlStatePrefix)) {
                return true;
            }
            current = current.getNextException();
        }
        return false;
    }

    private boolean containsTimeoutCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof java.net.SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean containsCause(Throwable throwable, Class<? extends Throwable> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}

