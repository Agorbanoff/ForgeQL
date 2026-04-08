package com.example.common.exceptions;

import com.example.common.CustomException;
import org.springframework.http.HttpStatus;

public class SchemaTableNotFoundException extends CustomException {
    public SchemaTableNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
