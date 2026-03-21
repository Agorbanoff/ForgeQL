package com.example.SigmaQL.common.exceptions;

import com.example.SigmaQL.common.CustomException;
import org.springframework.http.HttpStatus;

public class NoDataSourceFoundException extends CustomException {
    public NoDataSourceFoundException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
