package com.example.common.exceptions;

import com.example.common.CustomException;
import org.springframework.http.HttpStatus;

public class PostgresMetadataIntrospectionException extends CustomException {
    public PostgresMetadataIntrospectionException(String message) {
        super(message, HttpStatus.BAD_GATEWAY);
    }

    public PostgresMetadataIntrospectionException(String message, Throwable cause) {
        super(message, HttpStatus.BAD_GATEWAY, cause);
    }
}
