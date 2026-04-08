package com.example.core.postgres.introspection;

import com.example.core.postgres.connection.PostgresRuntimeConnectionDefinition;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UniqueConstraintMetadataLoader {

    public List<UniqueConstraintMetadata> load(PostgresRuntimeConnectionDefinition definition) {
        throw new UnsupportedOperationException("Unique constraint metadata loading is not implemented yet for datasource " + definition.datasourceId());
    }
}

