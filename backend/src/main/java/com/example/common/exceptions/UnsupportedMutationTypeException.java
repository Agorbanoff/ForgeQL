package com.example.common.exceptions;

import com.example.common.CustomException;
import org.springframework.http.HttpStatus;

public class UnsupportedMutationTypeException extends CustomException {
    public UnsupportedMutationTypeException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
