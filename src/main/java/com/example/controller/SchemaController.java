package com.example.controller;

import com.example.common.exceptions.InvalidQueryException;
import com.example.registry.SchemaRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SchemaController {

    private final SchemaRegistry schemaRegistry;

    public SchemaController(SchemaRegistry schemaRegistry) {
        this.schemaRegistry = schemaRegistry;
    }

    @GetMapping("/schema/users")
    public Object usersSchema() throws InvalidQueryException {
        return schemaRegistry.getEntity("users");
    }
}