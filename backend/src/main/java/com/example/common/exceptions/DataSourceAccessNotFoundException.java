package com.example.common.exceptions;

import com.example.common.CustomException;
import org.springframework.http.HttpStatus;

public class DataSourceAccessNotFoundException extends CustomException {
    public DataSourceAccessNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
