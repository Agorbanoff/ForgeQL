package com.example.common.exceptions;

import com.example.common.CustomException;
import org.springframework.http.HttpStatus;

public class InvalidDataSourceConfigurationException extends CustomException {
    public InvalidDataSourceConfigurationException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
