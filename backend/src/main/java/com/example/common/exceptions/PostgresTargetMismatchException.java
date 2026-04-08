package com.example.common.exceptions;

import com.example.common.CustomException;
import org.springframework.http.HttpStatus;

public class PostgresTargetMismatchException extends CustomException {
    public PostgresTargetMismatchException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
