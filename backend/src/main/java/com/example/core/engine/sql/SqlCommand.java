package com.example.core.engine.sql;

import java.util.List;

public record SqlCommand(
        String sql,
        List<Object> parameters
) {
}
