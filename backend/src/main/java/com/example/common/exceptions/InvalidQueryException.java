package com.example.common.exceptions;

import com.example.common.CustomException;
import org.springframework.http.HttpStatus;

public class InvalidQueryException extends CustomException {
    public InvalidQueryException(String message, HttpStatus statusCode) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
