package com.example.common;

import org.springframework.stereotype.Component;

import javax.net.ssl.SSLException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.sql.SQLException;
import java.sql.SQLInvalidAuthorizationSpecException;
import java.sql.SQLTimeoutException;
import java.util.Locale;

@Component
public class PostgresJdbcErrorClassifier {

    public ErrorCategory classify(Throwable throwable) {
        if (throwable == null) {
            return ErrorCategory.UNEXPECTED_FAILURE;
        }
        if (isTimeout(throwable)) {
            return ErrorCategory.TIMEOUT;
        }
        if (isSslFailure(throwable)) {
            return ErrorCategory.SSL_FAILURE;
        }
        if (isAuthenticationFailure(throwable)) {
            return ErrorCategory.AUTHENTICATION_FAILURE;
        }
        if (hasSqlState(throwable, "23505")) {
            return ErrorCategory.UNIQUE_CONSTRAINT_VIOLATION;
        }
        if (hasSqlState(throwable, "23503")) {
            return ErrorCategory.FOREIGN_KEY_VIOLATION;
        }
        if (isValidationFailure(throwable)) {
            return ErrorCategory.VALIDATION_FAILURE;
        }
        if (isConnectionFailure(throwable)) {
            return ErrorCategory.CONNECTION_FAILURE;
        }
        return ErrorCategory.UNEXPECTED_FAILURE;
    }

    private boolean isTimeout(Throwable throwable) {
        return containsCause(throwable, SQLTimeoutException.class)
                || containsCause(throwable, SocketTimeoutException.class)
                || hasSqlState(throwable, "57014");
    }

    private boolean isSslFailure(Throwable throwable) {
        if (containsCause(throwable, SSLException.class)
                || containsCause(throwable, CertificateException.class)
                || containsCause(throwable, CertPathValidatorException.class)) {
            return true;
        }

        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains("ssl")) {
                return true;
            }
            current = current.getCause();
        }

        return false;
    }

    private boolean isAuthenticationFailure(Throwable throwable) {
        return containsCause(throwable, SQLInvalidAuthorizationSpecException.class)
                || hasSqlStatePrefix(throwable, "28");
    }

    private boolean isValidationFailure(Throwable throwable) {
        return hasSqlStatePrefix(throwable, "22")
                || hasSqlState(throwable, "23502")
                || hasSqlState(throwable, "23514");
    }

    private boolean isConnectionFailure(Throwable throwable) {
        return hasSqlStatePrefix(throwable, "08")
                || containsCause(throwable, ConnectException.class);
    }

    private boolean hasSqlState(Throwable throwable, String sqlState) {
        SQLException sqlException = firstSqlException(throwable);
        while (sqlException != null) {
            if (sqlState.equals(sqlException.getSQLState())) {
                return true;
            }
            sqlException = sqlException.getNextException();
        }

        Throwable cause = throwable.getCause();
        return cause != null && hasSqlState(cause, sqlState);
    }

    private boolean hasSqlStatePrefix(Throwable throwable, String sqlStatePrefix) {
        SQLException sqlException = firstSqlException(throwable);
        while (sqlException != null) {
            String sqlState = sqlException.getSQLState();
            if (sqlState != null && sqlState.startsWith(sqlStatePrefix)) {
                return true;
            }
            sqlException = sqlException.getNextException();
        }

        Throwable cause = throwable.getCause();
        return cause != null && hasSqlStatePrefix(cause, sqlStatePrefix);
    }

    private SQLException firstSqlException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sqlException) {
                return sqlException;
            }
            current = current.getCause();
        }
        return null;
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

    public enum ErrorCategory {
        TIMEOUT,
        SSL_FAILURE,
        AUTHENTICATION_FAILURE,
        UNIQUE_CONSTRAINT_VIOLATION,
        FOREIGN_KEY_VIOLATION,
        VALIDATION_FAILURE,
        CONNECTION_FAILURE,
        UNEXPECTED_FAILURE
    }
}
