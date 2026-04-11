package com.example.core.postgres.schema.registry;

import com.example.common.exceptions.InvalidSchemaSnapshotException;
import com.example.core.postgres.schema.model.GeneratedSchema;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SchemaRegistryService {

    private final Map<Integer, GeneratedSchema> snapshotsByDatasourceId = new ConcurrentHashMap<>();

    public GeneratedSchema register(GeneratedSchema snapshot) {
        if (snapshot == null) {
            throw new InvalidSchemaSnapshotException("Generated schema is required");
        }
        if (snapshot.datasourceId() == null) {
            throw new InvalidSchemaSnapshotException("Generated schema datasource id is required");
        }
        if (snapshot.fingerprint() == null || snapshot.fingerprint().isBlank()) {
            throw new InvalidSchemaSnapshotException("Generated schema fingerprint is required");
        }

        snapshotsByDatasourceId.put(snapshot.datasourceId(), snapshot);
        return snapshot;
    }

    public Optional<GeneratedSchema> findByDatasourceId(Integer datasourceId) {
        return Optional.ofNullable(snapshotsByDatasourceId.get(datasourceId));
    }

    public void evict(Integer datasourceId) {
        snapshotsByDatasourceId.remove(datasourceId);
    }

    public boolean contains(Integer datasourceId) {
        return snapshotsByDatasourceId.containsKey(datasourceId);
    }
}

