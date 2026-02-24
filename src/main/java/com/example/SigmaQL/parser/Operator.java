package com.example.SigmaQL.parser;

import com.example.SigmaQL.common.exceptions.InvalidQueryException;
import org.springframework.http.HttpStatus;

public enum Operator {
    EQ("eq"),
    NEQ("neq"),
    GT("gt"),
    GTE("gte"),
    LT("lt"),
    LTE("lte"),
    IN("in"),
    BETWEEN("between");

    private final String key;

    Operator(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static Operator fromKey(String key) throws InvalidQueryException {
        if (key == null) {
            throw new InvalidQueryException("Operator is null", HttpStatus.BAD_REQUEST);
        }

        for (Operator op : values()) {
            if (op.key.equalsIgnoreCase(key)) {
                return op;
            }
        }

        throw new InvalidQueryException("Unknown operator: " + key, HttpStatus.BAD_REQUEST);
    }
}