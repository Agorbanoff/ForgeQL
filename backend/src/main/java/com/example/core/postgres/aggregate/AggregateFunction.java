package com.example.core.postgres.aggregate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum AggregateFunction {
    COUNT("count"),
    SUM("sum"),
    AVG("avg"),
    MIN("min"),
    MAX("max");

    private final String wireName;

    AggregateFunction(String wireName) {
        this.wireName = wireName;
    }

    @JsonValue
    public String wireName() {
        return wireName;
    }

    @JsonCreator
    public static AggregateFunction fromWireValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalizedValue = value.trim().toLowerCase(Locale.ROOT);
        for (AggregateFunction function : values()) {
            if (function.wireName.equals(normalizedValue)) {
                return function;
            }
        }

        throw new IllegalArgumentException("Unsupported aggregate function: " + value);
    }
}

