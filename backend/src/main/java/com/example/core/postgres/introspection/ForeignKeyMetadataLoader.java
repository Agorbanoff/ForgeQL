package com.example.core.postgres.introspection;

import com.example.core.postgres.connection.PostgresRuntimeConnectionDefinition;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ForeignKeyMetadataLoader {

    public List<ForeignKeyMetadata> load(PostgresRuntimeConnectionDefinition definition) {
        throw new UnsupportedOperationException("Foreign key metadata loading is not implemented yet for datasource " + definition.datasourceId());
    }
}

