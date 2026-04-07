package com.example.core.engine.schema.registry;

import com.example.core.engine.schema.model.PostgresSchemaSnapshot;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SchemaRegistryService {

    private final Map<Integer, PostgresSchemaSnapshot> snapshotsByDatasourceId = new ConcurrentHashMap<>();

    public PostgresSchemaSnapshot register(PostgresSchemaSnapshot snapshot) {
        snapshotsByDatasourceId.put(snapshot.datasourceId(), snapshot);
        return snapshot;
    }

    public Optional<PostgresSchemaSnapshot> findByDatasourceId(Integer datasourceId) {
        return Optional.ofNullable(snapshotsByDatasourceId.get(datasourceId));
    }

    public void evict(Integer datasourceId) {
        snapshotsByDatasourceId.remove(datasourceId);
    }

    public boolean contains(Integer datasourceId) {
        return snapshotsByDatasourceId.containsKey(datasourceId);
    }
}
