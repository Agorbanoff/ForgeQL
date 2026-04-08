package com.example.core.postgres.api.dto.response;

import com.example.core.postgres.schema.model.SchemaRelation;

import java.util.List;

public record RelationsResponse(
        List<SchemaRelation> relations
) {
}
