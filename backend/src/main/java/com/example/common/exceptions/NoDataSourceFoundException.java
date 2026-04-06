package com.example.common.exceptions;

import com.example.common.CustomException;
import org.springframework.http.HttpStatus;

public class NoDataSourceFoundException extends CustomException {
    public NoDataSourceFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
