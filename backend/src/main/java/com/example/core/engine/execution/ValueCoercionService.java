package com.example.core.engine.execution;

import org.springframework.stereotype.Service;

@Service
public class ValueCoercionService {

    public Object coerce(String dbType, Object value) {
        throw new UnsupportedOperationException("Value coercion is not implemented yet for dbType " + dbType);
    }
}
