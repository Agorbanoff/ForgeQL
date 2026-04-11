package com.example.core.postgres.schema;

import com.example.common.exceptions.AmbiguousTableIdentifierException;
import com.example.common.exceptions.MissingRequiredFieldException;
import com.example.common.exceptions.SchemaTableNotFoundException;
import com.example.core.postgres.schema.model.GeneratedSchema;
import com.example.core.postgres.schema.model.SchemaTable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PublicTableIdentifierResolver {

    public ResolvedTableIdentifier resolve(GeneratedSchema schema, String tableIdentifier) {
        if (schema == null) {
            throw new SchemaTableNotFoundException("Schema is required for table resolution");
        }
        if (tableIdentifier == null || tableIdentifier.isBlank()) {
            throw new MissingRequiredFieldException("tableName is required");
        }

        String normalizedIdentifier = tableIdentifier.trim();
        if (normalizedIdentifier.contains(".")) {
            SchemaTable qualifiedTable = schema.tables().get(normalizedIdentifier);
            if (qualifiedTable == null) {
                throw new SchemaTableNotFoundException("Table not found: " + normalizedIdentifier);
            }
            return new ResolvedTableIdentifier(
                    normalizedIdentifier,
                    qualifiedTable.qualifiedName(),
                    qualifiedTable
            );
        }

        List<SchemaTable> matches = new ArrayList<>();
        for (SchemaTable table : schema.tables().values()) {
            if (table.name().equals(normalizedIdentifier)) {
                matches.add(table);
            }
        }

        if (matches.isEmpty()) {
            throw new SchemaTableNotFoundException("Table not found: " + normalizedIdentifier);
        }
        if (matches.size() > 1) {
            throw new AmbiguousTableIdentifierException(
                    "Table identifier '" + normalizedIdentifier
                            + "' is ambiguous. Use a schema-qualified identifier such as public."
                            + normalizedIdentifier
            );
        }

        SchemaTable resolvedTable = matches.get(0);
        return new ResolvedTableIdentifier(
                normalizedIdentifier,
                resolvedTable.qualifiedName(),
                resolvedTable
        );
    }
}
