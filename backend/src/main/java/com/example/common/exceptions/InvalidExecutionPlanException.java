package com.example.common.exceptions;

import com.example.common.CustomException;
import org.springframework.http.HttpStatus;

public class InvalidExecutionPlanException extends CustomException {
    public InvalidExecutionPlanException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
