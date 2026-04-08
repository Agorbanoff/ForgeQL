package com.example.common.exceptions;

import com.example.common.CustomException;
import org.springframework.http.HttpStatus;

public class AmbiguousTableIdentifierException extends CustomException {
    public AmbiguousTableIdentifierException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
