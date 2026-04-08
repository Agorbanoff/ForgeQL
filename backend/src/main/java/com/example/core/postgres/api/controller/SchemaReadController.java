package com.example.core.postgres.api.controller;

import com.example.auth.filter.AuthenticatedUser;
import com.example.core.postgres.api.dto.response.SchemaResponse;
import com.example.core.postgres.api.dto.response.SchemaSummaryResponse;
import com.example.core.postgres.schema.SchemaReadService;
import com.example.core.postgres.schema.model.GeneratedSchema;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/core/datasources/{datasourceId}/schema")
public class SchemaReadController {

    private final SchemaReadService schemaReadService;

    public SchemaReadController(SchemaReadService schemaReadService) {
        this.schemaReadService = schemaReadService;
    }

    @GetMapping
    public ResponseEntity<SchemaResponse> getSchema(
            @PathVariable Integer datasourceId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        GeneratedSchema schema = schemaReadService.getSchema(datasourceId, authenticatedUser.userId());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new SchemaResponse(schema));
    }

    @GetMapping("/summary")
    public ResponseEntity<SchemaSummaryResponse> getSchemaSummary(
            @PathVariable Integer datasourceId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        GeneratedSchema schema = schemaReadService.getSchema(datasourceId, authenticatedUser.userId());

        SchemaSummaryResponse response = new SchemaSummaryResponse(
                schema.datasourceId(),
                schema.fingerprint(),
                schema.generatedAt(),
                schema.serverVersion(),
                schema.tables().size()
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }
}
