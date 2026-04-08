package com.example.common.exceptions;

import com.example.common.CustomException;
import org.springframework.http.HttpStatus;

public class MissingRequiredFieldException extends CustomException {
    public MissingRequiredFieldException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
