package com.example.SigmaQL.common.exceptions;

import com.example.SigmaQL.common.CustomException;
import org.springframework.http.HttpStatus;

public class InvalidQueryException extends CustomException {
    public InvalidQueryException(String message, HttpStatus statusCode) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
