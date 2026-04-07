package com.example.core.engine.api.controller;

import com.example.core.engine.schema.SchemaGenerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/core/datasources/{datasourceId}/schema")
public class SchemaGenerationController {

    private final SchemaGenerationService schemaGenerationService;

    @Autowired
    public SchemaGenerationController(SchemaGenerationService schemaGenerationService) {
        this.schemaGenerationService = schemaGenerationService;
    }
}
