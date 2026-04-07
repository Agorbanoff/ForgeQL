package com.example.core.engine.introspection;

import com.example.core.engine.connection.PostgresRuntimeConnectionDefinition;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ForeignKeyMetadataLoader {

    public List<ForeignKeyMetadata> load(PostgresRuntimeConnectionDefinition definition) {
        throw new UnsupportedOperationException("Foreign key metadata loading is not implemented yet for datasource " + definition.datasourceId());
    }
}
