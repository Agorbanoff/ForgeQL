package com.example.common.exceptions;

import com.example.common.CustomException;
import org.springframework.http.HttpStatus;

public class PostgresConnectionTimeoutException extends CustomException {
    public PostgresConnectionTimeoutException(String message) {
        super(message, HttpStatus.GATEWAY_TIMEOUT);
    }
}
