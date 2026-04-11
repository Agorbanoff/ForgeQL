package com.example.core.postgres.sql;

import java.util.List;

public record SqlCommand(
        String sql,
        List<Object> parameters
) {
    public SqlCommand {
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
    }
}

