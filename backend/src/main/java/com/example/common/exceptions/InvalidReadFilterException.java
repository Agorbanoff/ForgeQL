package com.example.common.exceptions;

import com.example.common.CustomException;
import org.springframework.http.HttpStatus;

public class InvalidReadFilterException extends CustomException {
    public InvalidReadFilterException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
