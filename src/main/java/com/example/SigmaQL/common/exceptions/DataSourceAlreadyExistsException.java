package com.example.SigmaQL.common.exceptions;

import com.example.SigmaQL.common.CustomException;
import org.springframework.http.HttpStatus;

public class DataSourceAlreadyExistsException extends CustomException {
    public DataSourceAlreadyExistsException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
