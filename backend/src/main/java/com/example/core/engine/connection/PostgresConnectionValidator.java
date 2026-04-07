package com.example.core.engine.connection;

import com.example.common.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class PostgresConnectionValidator {

    public void validateProductName(String databaseProductName) {
        if (!"PostgreSQL".equalsIgnoreCase(databaseProductName)) {
            throw new CustomException("Target datasource is not PostgreSQL", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }
}
