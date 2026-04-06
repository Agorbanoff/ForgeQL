package com.example.common.exceptions;

import com.example.common.CustomException;
import org.springframework.http.HttpStatus;

public class EmailAlreadyInUseException extends CustomException {
    public EmailAlreadyInUseException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
