package com.example.SigmaQL.registry;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntitySchema {
    private String table;
    private String primaryKey;
    private Set<String> fields;
    private Map<String, RelationSchema> relations;
}