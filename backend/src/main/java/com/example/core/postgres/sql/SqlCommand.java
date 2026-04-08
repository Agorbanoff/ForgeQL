package com.example.core.postgres.sql;

import java.util.List;

public record SqlCommand(
        String sql,
        List<Object> parameters
) {
}

