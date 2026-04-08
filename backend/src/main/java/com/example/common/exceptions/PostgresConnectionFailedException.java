package com.example.common.exceptions;

import com.example.common.CustomException;
import org.springframework.http.HttpStatus;

public class PostgresConnectionFailedException extends CustomException {
    public PostgresConnectionFailedException(String message) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
