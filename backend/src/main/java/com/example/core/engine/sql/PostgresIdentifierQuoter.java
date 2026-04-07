package com.example.core.engine.sql;

import org.springframework.stereotype.Component;

@Component
public class PostgresIdentifierQuoter {

    public String quote(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
