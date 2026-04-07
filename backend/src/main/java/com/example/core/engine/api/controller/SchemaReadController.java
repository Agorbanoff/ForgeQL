package com.example.core.engine.api.controller;

import com.example.core.engine.schema.registry.SchemaRegistryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/core/datasources/{datasourceId}/schema")
public class SchemaReadController {

    private final SchemaRegistryService schemaRegistryService;

    @Autowired
    public SchemaReadController(SchemaRegistryService schemaRegistryService) {
        this.schemaRegistryService = schemaRegistryService;
    }
}
