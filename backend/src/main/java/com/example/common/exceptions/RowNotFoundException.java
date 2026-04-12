package com.example.common.exceptions;

import com.example.common.CustomException;
import org.springframework.http.HttpStatus;

public class RowNotFoundException extends CustomException {
    public RowNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
