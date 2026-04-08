package com.example.common.exceptions;

import com.example.common.CustomException;
import org.springframework.http.HttpStatus;

public class PostgresAuthenticationFailedException extends CustomException {
    public PostgresAuthenticationFailedException(String message) {
        super(message, HttpStatus.BAD_GATEWAY);
    }
}
