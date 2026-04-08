package com.example.common.exceptions;

import com.example.common.CustomException;
import org.springframework.http.HttpStatus;

public class GeneratedSchemaNotFoundException extends CustomException {
    public GeneratedSchemaNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
