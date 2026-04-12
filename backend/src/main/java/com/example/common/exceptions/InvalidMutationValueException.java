package com.example.common.exceptions;

import com.example.common.CustomException;
import org.springframework.http.HttpStatus;

public class InvalidMutationValueException extends CustomException {
    public InvalidMutationValueException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
