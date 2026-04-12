package com.example.common.exceptions;

import com.example.common.CustomException;
import org.springframework.http.HttpStatus;

public class PostgresPoolLimitExceededException extends CustomException {
    public PostgresPoolLimitExceededException(String message) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
