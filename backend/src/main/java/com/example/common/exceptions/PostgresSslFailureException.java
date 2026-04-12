package com.example.common.exceptions;

import com.example.common.CustomException;
import org.springframework.http.HttpStatus;

public class PostgresSslFailureException extends CustomException {
    public PostgresSslFailureException(String message) {
        super(message, HttpStatus.BAD_GATEWAY);
    }
}
