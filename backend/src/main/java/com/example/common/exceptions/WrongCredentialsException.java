package com.example.common.exceptions;

import com.example.common.CustomException;
import org.springframework.http.HttpStatus;

public class WrongCredentialsException extends CustomException {
    public WrongCredentialsException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
