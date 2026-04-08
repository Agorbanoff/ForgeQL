package com.example.common.exceptions;

import com.example.common.CustomException;
import org.springframework.http.HttpStatus;

public class InvalidExtraJdbcOptionsException extends CustomException {
    public InvalidExtraJdbcOptionsException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
