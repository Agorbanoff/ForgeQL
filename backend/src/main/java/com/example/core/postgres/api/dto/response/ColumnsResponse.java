package com.example.core.postgres.api.dto.response;

import com.example.core.postgres.schema.model.SchemaColumn;

import java.util.List;

public record ColumnsResponse(
        List<SchemaColumn> columns
) {
}
