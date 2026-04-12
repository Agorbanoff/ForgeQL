package com.example.common;

import com.example.common.PostgresJdbcErrorClassifier.ErrorCategory;
import com.example.common.exceptions.AmbiguousTableIdentifierException;
import com.example.common.exceptions.GeneratedSchemaNotFoundException;
import com.example.common.exceptions.InvalidAggregateRequestException;
import com.example.common.exceptions.InvalidDataSourceConfigurationException;
import com.example.common.exceptions.InvalidExecutionPlanException;
import com.example.common.exceptions.InvalidMutationValueException;
import com.example.common.exceptions.InvalidReadFilterException;
import com.example.common.exceptions.InvalidSchemaSnapshotException;
import com.example.common.exceptions.MissingRequiredFieldException;
import com.example.common.exceptions.NoDataSourceFoundException;
import com.example.common.exceptions.PostgresAuthenticationFailedException;
import com.example.common.exceptions.PostgresConnectionFingerprintException;
import com.example.common.exceptions.PostgresConnectionFailedException;
import com.example.common.exceptions.PostgresConnectionTimeoutException;
import com.example.common.exceptions.PostgresMetadataIntrospectionException;
import com.example.common.exceptions.PostgresMutationExecutionException;
import com.example.common.exceptions.PostgresPoolLimitExceededException;
import com.example.common.exceptions.PostgresQueryExecutionException;
import com.example.common.exceptions.PostgresSslFailureException;
import com.example.common.exceptions.PostgresTargetMismatchException;
import com.example.common.exceptions.RowNotFoundException;
import com.example.common.exceptions.SchemaFingerprintComputationException;
import com.example.common.exceptions.SchemaSerializationException;
import com.example.common.exceptions.SchemaTableNotFoundException;
import com.example.common.exceptions.UnsupportedDatabaseTypeException;
import com.example.common.exceptions.UnsupportedMutationTargetException;
import com.example.common.exceptions.UnsupportedMutationTypeException;
import com.example.controller.dtos.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final PostgresJdbcErrorClassifier postgresJdbcErrorClassifier;

    public GlobalExceptionHandler(PostgresJdbcErrorClassifier postgresJdbcErrorClassifier) {
        this.postgresJdbcErrorClassifier = postgresJdbcErrorClassifier;
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiErrorResponse> handleCustomException(
            CustomException exception,
            HttpServletRequest request
    ) {
        return buildResponse(resolveCustomException(exception), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .distinct()
                .collect(Collectors.joining("; "));

        if (message.isBlank()) {
            message = "Validation failed";
        }

        return buildResponse(
                new ErrorDescriptor(HttpStatus.BAD_REQUEST, ApplicationErrorCode.VALIDATION_ERROR, message),
                request
        );
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequestExceptions(
            Exception exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                new ErrorDescriptor(
                        HttpStatus.BAD_REQUEST,
                        ApplicationErrorCode.VALIDATION_ERROR,
                        "Request parameter is invalid"
                ),
                request
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                new ErrorDescriptor(
                        HttpStatus.BAD_REQUEST,
                        ApplicationErrorCode.VALIDATION_ERROR,
                        "Malformed JSON request"
                ),
                request
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        return buildResponse(resolveSpringDataException(exception), request);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiErrorResponse> handleDataAccessException(
            DataAccessException exception,
            HttpServletRequest request
    ) {
        return buildResponse(resolveSpringDataException(exception), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDeniedException(
            AccessDeniedException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                new ErrorDescriptor(
                        HttpStatus.FORBIDDEN,
                        ApplicationErrorCode.ACCESS_DENIED,
                        "Access is denied"
                ),
                request
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFoundException(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                new ErrorDescriptor(
                        HttpStatus.NOT_FOUND,
                        ApplicationErrorCode.RESOURCE_NOT_FOUND,
                        "Resource not found"
                ),
                request
        );
    }

    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ApiErrorResponse> handleErrorResponseException(
            ErrorResponseException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        return buildResponse(
                new ErrorDescriptor(
                        status,
                        mapGenericStatusCode(status),
                        resolveMessage(exception, exception.getBody().getDetail())
                ),
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleAnyException(
            Exception exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                new ErrorDescriptor(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        ApplicationErrorCode.INTERNAL_SERVER_ERROR,
                        "Something went wrong"
                ),
                request
        );
    }

    private ErrorDescriptor resolveCustomException(CustomException exception) {
        if (exception instanceof NoDataSourceFoundException) {
            return new ErrorDescriptor(
                    exception.getStatusCode(),
                    ApplicationErrorCode.DATASOURCE_NOT_FOUND,
                    exception.getMessage()
            );
        }
        if (exception instanceof SchemaTableNotFoundException) {
            return new ErrorDescriptor(
                    exception.getStatusCode(),
                    ApplicationErrorCode.TABLE_NOT_FOUND,
                    exception.getMessage()
            );
        }
        if (exception instanceof RowNotFoundException) {
            return new ErrorDescriptor(
                    exception.getStatusCode(),
                    ApplicationErrorCode.ROW_NOT_FOUND,
                    exception.getMessage()
            );
        }
        if (exception instanceof GeneratedSchemaNotFoundException) {
            return new ErrorDescriptor(
                    exception.getStatusCode(),
                    ApplicationErrorCode.GENERATED_SCHEMA_NOT_FOUND,
                    exception.getMessage()
            );
        }
        if (exception instanceof AmbiguousTableIdentifierException) {
            return new ErrorDescriptor(
                    exception.getStatusCode(),
                    ApplicationErrorCode.AMBIGUOUS_TABLE_IDENTIFIER,
                    exception.getMessage()
            );
        }
        if (exception instanceof PostgresConnectionTimeoutException) {
            return new ErrorDescriptor(
                    exception.getStatusCode(),
                    ApplicationErrorCode.POSTGRES_TIMEOUT,
                    "Connection to PostgreSQL timed out"
            );
        }
        if (exception instanceof PostgresSslFailureException) {
            return new ErrorDescriptor(
                    exception.getStatusCode(),
                    ApplicationErrorCode.POSTGRES_SSL_FAILURE,
                    "SSL negotiation with PostgreSQL failed"
            );
        }
        if (exception instanceof PostgresAuthenticationFailedException) {
            return new ErrorDescriptor(
                    exception.getStatusCode(),
                    ApplicationErrorCode.POSTGRES_AUTHENTICATION_FAILURE,
                    "Authentication with PostgreSQL failed"
            );
        }
        if (exception instanceof PostgresConnectionFailedException) {
            return new ErrorDescriptor(
                    exception.getStatusCode(),
                    ApplicationErrorCode.POSTGRES_CONNECTION_FAILURE,
                    "Connection to PostgreSQL failed"
            );
        }
        if (exception instanceof PostgresConnectionFingerprintException) {
            return new ErrorDescriptor(
                    exception.getStatusCode(),
                    ApplicationErrorCode.UNEXPECTED_POSTGRES_FAILURE,
                    "Failed to prepare PostgreSQL runtime connection"
            );
        }
        if (exception instanceof PostgresPoolLimitExceededException) {
            return new ErrorDescriptor(
                    exception.getStatusCode(),
                    ApplicationErrorCode.POSTGRES_POOL_LIMIT_EXCEEDED,
                    "PostgreSQL runtime pool capacity has been reached"
            );
        }
        if (exception instanceof PostgresMetadataIntrospectionException
                || exception instanceof SchemaSerializationException
                || exception instanceof SchemaFingerprintComputationException
                || exception instanceof InvalidSchemaSnapshotException) {
            return resolveSchemaGenerationException(exception);
        }
        if (exception instanceof PostgresQueryExecutionException
                || exception instanceof PostgresMutationExecutionException) {
            return resolvePostgresExecutionException(exception);
        }
        if (exception instanceof UnsupportedMutationTargetException) {
            return new ErrorDescriptor(
                    exception.getStatusCode(),
                    ApplicationErrorCode.UNSUPPORTED_MUTATION_TARGET,
                    exception.getMessage()
            );
        }
        if (exception instanceof UnsupportedDatabaseTypeException) {
            return new ErrorDescriptor(
                    exception.getStatusCode(),
                    ApplicationErrorCode.UNSUPPORTED_DATABASE_TYPE,
                    exception.getMessage()
            );
        }
        if (exception instanceof UnsupportedMutationTypeException) {
            return new ErrorDescriptor(
                    exception.getStatusCode(),
                    ApplicationErrorCode.UNSUPPORTED_MUTATION_TYPE,
                    exception.getMessage()
            );
        }
        if (exception instanceof PostgresTargetMismatchException) {
            return new ErrorDescriptor(
                    exception.getStatusCode(),
                    ApplicationErrorCode.UNSUPPORTED_POSTGRES_TARGET,
                    exception.getMessage()
            );
        }
        if (exception instanceof InvalidExecutionPlanException
                || exception instanceof InvalidAggregateRequestException
                || exception instanceof InvalidReadFilterException
                || exception instanceof InvalidMutationValueException
                || exception instanceof MissingRequiredFieldException
                || exception instanceof InvalidDataSourceConfigurationException) {
            return new ErrorDescriptor(
                    exception.getStatusCode(),
                    ApplicationErrorCode.VALIDATION_ERROR,
                    exception.getMessage()
            );
        }

        return new ErrorDescriptor(
                exception.getStatusCode(),
                mapGenericStatusCode(exception.getStatusCode()),
                exception.getMessage()
        );
    }

    private ErrorDescriptor resolvePostgresExecutionException(CustomException exception) {
        ErrorCategory errorCategory = postgresJdbcErrorClassifier.classify(exception.getCause());
        return switch (errorCategory) {
            case TIMEOUT -> new ErrorDescriptor(
                    HttpStatus.GATEWAY_TIMEOUT,
                    ApplicationErrorCode.POSTGRES_TIMEOUT,
                    "PostgreSQL operation timed out"
            );
            case SSL_FAILURE -> new ErrorDescriptor(
                    HttpStatus.BAD_GATEWAY,
                    ApplicationErrorCode.POSTGRES_SSL_FAILURE,
                    "SSL negotiation with PostgreSQL failed"
            );
            case AUTHENTICATION_FAILURE -> new ErrorDescriptor(
                    HttpStatus.BAD_GATEWAY,
                    ApplicationErrorCode.POSTGRES_AUTHENTICATION_FAILURE,
                    "Authentication with PostgreSQL failed"
            );
            case UNIQUE_CONSTRAINT_VIOLATION -> new ErrorDescriptor(
                    HttpStatus.CONFLICT,
                    ApplicationErrorCode.UNIQUE_CONSTRAINT_VIOLATION,
                    "Unique constraint violated"
            );
            case FOREIGN_KEY_VIOLATION -> new ErrorDescriptor(
                    HttpStatus.CONFLICT,
                    ApplicationErrorCode.FOREIGN_KEY_VIOLATION,
                    "Foreign key constraint violated"
            );
            case VALIDATION_FAILURE -> new ErrorDescriptor(
                    HttpStatus.BAD_REQUEST,
                    ApplicationErrorCode.POSTGRES_VALIDATION_FAILURE,
                    "PostgreSQL rejected the request values"
            );
            case CONNECTION_FAILURE -> new ErrorDescriptor(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    ApplicationErrorCode.POSTGRES_CONNECTION_FAILURE,
                    "Connection to PostgreSQL failed"
            );
            case UNEXPECTED_FAILURE -> new ErrorDescriptor(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ApplicationErrorCode.UNEXPECTED_POSTGRES_FAILURE,
                    "Unexpected PostgreSQL failure"
            );
        };
    }

    private ErrorDescriptor resolveSchemaGenerationException(CustomException exception) {
        if (exception instanceof PostgresMetadataIntrospectionException) {
            ErrorCategory errorCategory = postgresJdbcErrorClassifier.classify(exception.getCause());
            return switch (errorCategory) {
                case TIMEOUT -> new ErrorDescriptor(
                        HttpStatus.GATEWAY_TIMEOUT,
                        ApplicationErrorCode.POSTGRES_TIMEOUT,
                        "PostgreSQL schema generation timed out"
                );
                case SSL_FAILURE -> new ErrorDescriptor(
                        HttpStatus.BAD_GATEWAY,
                        ApplicationErrorCode.POSTGRES_SSL_FAILURE,
                        "SSL negotiation with PostgreSQL failed"
                );
                case AUTHENTICATION_FAILURE -> new ErrorDescriptor(
                        HttpStatus.BAD_GATEWAY,
                        ApplicationErrorCode.POSTGRES_AUTHENTICATION_FAILURE,
                        "Authentication with PostgreSQL failed"
                );
                case CONNECTION_FAILURE -> new ErrorDescriptor(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        ApplicationErrorCode.POSTGRES_CONNECTION_FAILURE,
                        "Connection to PostgreSQL failed"
                );
                default -> new ErrorDescriptor(
                        HttpStatus.BAD_GATEWAY,
                        ApplicationErrorCode.POSTGRES_SCHEMA_GENERATION_FAILURE,
                        "Failed to generate PostgreSQL schema"
                );
            };
        }

        return new ErrorDescriptor(
                exception.getStatusCode(),
                ApplicationErrorCode.POSTGRES_SCHEMA_GENERATION_FAILURE,
                "Failed to generate PostgreSQL schema"
        );
    }

    private ErrorDescriptor resolveSpringDataException(Exception exception) {
        ErrorCategory errorCategory = postgresJdbcErrorClassifier.classify(exception);
        return switch (errorCategory) {
            case UNIQUE_CONSTRAINT_VIOLATION -> new ErrorDescriptor(
                    HttpStatus.CONFLICT,
                    ApplicationErrorCode.UNIQUE_CONSTRAINT_VIOLATION,
                    "Unique constraint violated"
            );
            case FOREIGN_KEY_VIOLATION -> new ErrorDescriptor(
                    HttpStatus.CONFLICT,
                    ApplicationErrorCode.FOREIGN_KEY_VIOLATION,
                    "Foreign key constraint violated"
            );
            default -> new ErrorDescriptor(
                    exception instanceof DataIntegrityViolationException
                            ? HttpStatus.CONFLICT
                            : HttpStatus.INTERNAL_SERVER_ERROR,
                    exception instanceof DataIntegrityViolationException
                            ? ApplicationErrorCode.DATABASE_CONSTRAINT_VIOLATION
                            : ApplicationErrorCode.DATABASE_OPERATION_FAILED,
                    exception instanceof DataIntegrityViolationException
                            ? "Database constraint violation"
                            : "Database operation failed"
            );
        };
    }

    private ApplicationErrorCode mapGenericStatusCode(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST, UNPROCESSABLE_ENTITY -> ApplicationErrorCode.VALIDATION_ERROR;
            case NOT_FOUND -> ApplicationErrorCode.RESOURCE_NOT_FOUND;
            case FORBIDDEN -> ApplicationErrorCode.ACCESS_DENIED;
            case UNAUTHORIZED -> ApplicationErrorCode.AUTHENTICATION_REQUIRED;
            default -> ApplicationErrorCode.INTERNAL_SERVER_ERROR;
        };
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            ErrorDescriptor errorDescriptor,
            HttpServletRequest request
    ) {
        ErrorContext errorContext = extractErrorContext(request);
        ApiErrorResponse response = new ApiErrorResponse(
                Instant.now(),
                errorDescriptor.status().value(),
                errorDescriptor.status().getReasonPhrase(),
                errorDescriptor.code().name(),
                errorDescriptor.message(),
                errorContext.datasourceId(),
                errorContext.targetPath()
        );

        return ResponseEntity
                .status(errorDescriptor.status())
                .body(response);
    }

    private String resolveMessage(Exception exception, String fallback) {
        if (exception.getMessage() == null || exception.getMessage().isBlank()) {
            return fallback;
        }

        return exception.getMessage();
    }

    @SuppressWarnings("unchecked")
    private ErrorContext extractErrorContext(HttpServletRequest request) {
        Integer datasourceId = null;
        Object rawTemplateVariables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (rawTemplateVariables instanceof Map<?, ?> templateVariables) {
            Object datasourceIdValue = templateVariables.get("datasourceId");
            if (datasourceIdValue != null) {
                try {
                    datasourceId = Integer.valueOf(datasourceIdValue.toString());
                } catch (NumberFormatException ignored) {
                    datasourceId = null;
                }
            }
        }

        return new ErrorContext(datasourceId, request.getRequestURI());
    }

    private record ErrorDescriptor(HttpStatus status, ApplicationErrorCode code, String message) {
    }

    private record ErrorContext(Integer datasourceId, String targetPath) {
    }
}
