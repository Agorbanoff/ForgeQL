package com.example.common.exceptions;

import com.example.common.CustomException;
import org.springframework.http.HttpStatus;

public class PostgresQueryExecutionException extends CustomException {
    public PostgresQueryExecutionException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
