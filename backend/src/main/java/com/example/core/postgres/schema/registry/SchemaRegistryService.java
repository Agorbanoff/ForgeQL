package com.example.core.postgres.schema.registry;

import com.example.core.postgres.schema.model.GeneratedSchema;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SchemaRegistryService {

    private final Map<Integer, GeneratedSchema> snapshotsByDatasourceId = new ConcurrentHashMap<>();

    public GeneratedSchema register(GeneratedSchema snapshot) {
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

