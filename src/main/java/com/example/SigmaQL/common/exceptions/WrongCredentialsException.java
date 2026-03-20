package com.example.SigmaQL.common.exceptions;

import com.example.SigmaQL.common.CustomException;
import org.springframework.http.HttpStatus;

public class WrongCredentialsException extends CustomException {
    public WrongCredentialsException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
