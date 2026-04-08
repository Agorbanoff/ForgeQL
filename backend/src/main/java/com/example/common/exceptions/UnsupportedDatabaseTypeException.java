package com.example.common.exceptions;

import com.example.common.CustomException;
import org.springframework.http.HttpStatus;

public class UnsupportedDatabaseTypeException extends CustomException {
    public UnsupportedDatabaseTypeException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
