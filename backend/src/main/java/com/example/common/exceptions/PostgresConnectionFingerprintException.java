package com.example.common.exceptions;

import com.example.common.CustomException;
import org.springframework.http.HttpStatus;

public class PostgresConnectionFingerprintException extends CustomException {
    public PostgresConnectionFingerprintException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public PostgresConnectionFingerprintException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
