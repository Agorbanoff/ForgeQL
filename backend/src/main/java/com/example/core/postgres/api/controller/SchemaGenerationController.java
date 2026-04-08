package com.example.core.postgres.api.controller;

import com.example.auth.filter.AuthenticatedUser;
import com.example.core.postgres.api.dto.response.SchemaResponse;
import com.example.core.postgres.schema.SchemaGenerationService;
import com.example.core.postgres.schema.model.GeneratedSchema;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/core/datasources/{datasourceId}/schema")
public class SchemaGenerationController {

    private final SchemaGenerationService schemaGenerationService;

    public SchemaGenerationController(SchemaGenerationService schemaGenerationService) {
        this.schemaGenerationService = schemaGenerationService;
    }

    @PostMapping("/generate")
    public ResponseEntity<SchemaResponse> generateSchema(
            @PathVariable Integer datasourceId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        GeneratedSchema generatedSchema = schemaGenerationService.generate(datasourceId, authenticatedUser.userId());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new SchemaResponse(generatedSchema));
    }

    @PostMapping("/refresh")
    public ResponseEntity<SchemaResponse> refreshSchema(
            @PathVariable Integer datasourceId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        GeneratedSchema generatedSchema = schemaGenerationService.generate(datasourceId, authenticatedUser.userId());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new SchemaResponse(generatedSchema));
    }
}
