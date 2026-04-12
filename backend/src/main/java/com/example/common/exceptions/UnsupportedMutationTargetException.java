package com.example.common.exceptions;

import com.example.common.CustomException;
import org.springframework.http.HttpStatus;

public class UnsupportedMutationTargetException extends CustomException {
    public UnsupportedMutationTargetException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
