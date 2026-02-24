package com.example.SigmaQL.registry;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchemaRoot {
    private Map<String, EntitySchema> entities;
}