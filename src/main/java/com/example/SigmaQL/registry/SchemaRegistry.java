package com.example.SigmaQL.registry;

import com.example.SigmaQL.common.exceptions.InvalidQueryException;
import com.example.SigmaQL.common.exceptions.UnknownFieldException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class SchemaRegistry {

    private final Map<String, EntitySchema> entities;

    public SchemaRegistry(ObjectMapper mapper) {
        this.entities = load(mapper).getEntities();
        if (this.entities == null || this.entities.isEmpty()) {
            throw new IllegalStateException("schema.json loaded but entities is empty");
        }
    }

    public EntitySchema getEntity(String entityName) throws InvalidQueryException {
        EntitySchema e = entities.get(entityName);
        if (e == null) {
            throw new InvalidQueryException("Unknown entity: " + entityName, HttpStatus.BAD_REQUEST);
        }
        return e;
    }

    public void assertFieldExists(String entityName, String fieldName) throws InvalidQueryException, UnknownFieldException {
        EntitySchema e = getEntity(entityName);

        if (e.getFields() == null || !e.getFields().contains(fieldName)) {
            throw new UnknownFieldException("Unknown field: " + fieldName, HttpStatus.BAD_REQUEST);
        }
    }

    public RelationSchema getRelation(String entityName, String relationName) throws InvalidQueryException {
        EntitySchema e = getEntity(entityName);

        if (e.getRelations() == null || !e.getRelations().containsKey(relationName)) {
            throw new InvalidQueryException("Unknown relation: " + relationName, HttpStatus.BAD_REQUEST);
        }

        return e.getRelations().get(relationName);
    }

    public String getTable(String entityName) throws InvalidQueryException {
        return getEntity(entityName).getTable();
    }

    public String getPrimaryKey(String entityName) throws InvalidQueryException {
        return getEntity(entityName).getPrimaryKey();
    }

    private SchemaRoot load(ObjectMapper mapper) {
        try {
            var res = new ClassPathResource("schema.json");
            return mapper.readValue(res.getInputStream(), SchemaRoot.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load schema.json: " + e.getMessage(), e);
        }
    }
}