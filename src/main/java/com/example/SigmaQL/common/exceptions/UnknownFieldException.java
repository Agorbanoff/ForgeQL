package com.example.SigmaQL.common.exceptions;

import com.example.SigmaQL.common.CustomException;
import org.springframework.http.HttpStatus;

public class UnknownFieldException extends CustomException {
    public UnknownFieldException(String message, HttpStatus statusCode) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
