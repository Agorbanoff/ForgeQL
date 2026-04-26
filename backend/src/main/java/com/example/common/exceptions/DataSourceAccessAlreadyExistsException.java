package com.example.common.exceptions;

import com.example.common.CustomException;
import org.springframework.http.HttpStatus;

public class DataSourceAccessAlreadyExistsException extends CustomException {
    public DataSourceAccessAlreadyExistsException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
