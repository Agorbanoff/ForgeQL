package com.example.common.exceptions;

import com.example.common.CustomException;
import org.springframework.http.HttpStatus;

public class UnknownFieldException extends CustomException {
    public UnknownFieldException(String message, HttpStatus statusCode) {
        super(message, statusCode);
    }
}
