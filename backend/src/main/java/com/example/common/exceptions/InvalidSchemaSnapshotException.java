package com.example.common.exceptions;

import com.example.common.CustomException;
import org.springframework.http.HttpStatus;

public class InvalidSchemaSnapshotException extends CustomException {
    public InvalidSchemaSnapshotException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
