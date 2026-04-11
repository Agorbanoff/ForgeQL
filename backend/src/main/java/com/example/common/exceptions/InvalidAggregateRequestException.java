package com.example.common.exceptions;

import com.example.common.CustomException;
import org.springframework.http.HttpStatus;

public class InvalidAggregateRequestException extends CustomException {
    public InvalidAggregateRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
