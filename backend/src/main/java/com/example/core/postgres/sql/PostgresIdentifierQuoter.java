package com.example.core.postgres.sql;

import com.example.common.exceptions.InvalidExecutionPlanException;
import org.springframework.stereotype.Component;

@Component
public class PostgresIdentifierQuoter {

    public String quote(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new InvalidExecutionPlanException("SQL identifier is required");
        }

        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    public String quoteQualified(String qualifiedIdentifier) {
        if (qualifiedIdentifier == null || qualifiedIdentifier.isBlank()) {
            throw new InvalidExecutionPlanException("Qualified SQL identifier is required");
        }

        String[] parts = qualifiedIdentifier.trim().split("\\.", -1);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new InvalidExecutionPlanException(
                    "Qualified SQL identifier must contain schema and object name"
            );
        }

        return quote(parts[0]) + "." + quote(parts[1]);
    }

    public String quoteColumnReference(String alias, String columnName) {
        return quote(alias) + "." + quote(columnName);
    }
}

