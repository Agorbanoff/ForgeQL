package com.example.core.postgres.api.controller;

import com.example.auth.filter.AuthenticatedUser;
import com.example.core.postgres.api.dto.response.ColumnsResponse;
import com.example.core.postgres.api.dto.response.RelationsResponse;
import com.example.core.postgres.api.dto.response.TableResponse;
import com.example.core.postgres.api.dto.response.TablesResponse;
import com.example.core.postgres.schema.SchemaReadService;
import com.example.core.postgres.schema.model.SchemaTable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/core/datasources/{datasourceId}/tables")
public class TableExploreController {

    private final SchemaReadService schemaReadService;

    public TableExploreController(SchemaReadService schemaReadService) {
        this.schemaReadService = schemaReadService;
    }

    @GetMapping
    public ResponseEntity<TablesResponse> getTables(
            @PathVariable Integer datasourceId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        List<SchemaTable> tables = schemaReadService.getTables(datasourceId, authenticatedUser.userId());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new TablesResponse(tables));
    }

    @GetMapping("/{tableName:.+}")
    public ResponseEntity<TableResponse> getTable(
            @PathVariable Integer datasourceId,
            @PathVariable String tableName,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        SchemaTable table = schemaReadService.getTable(datasourceId, authenticatedUser.userId(), tableName);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new TableResponse(table));
    }

    @GetMapping("/{tableName:.+}/columns")
    public ResponseEntity<ColumnsResponse> getTableColumns(
            @PathVariable Integer datasourceId,
            @PathVariable String tableName,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ColumnsResponse(
                        schemaReadService.getTableColumns(datasourceId, authenticatedUser.userId(), tableName)
                ));
    }

    @GetMapping("/{tableName:.+}/relations")
    public ResponseEntity<RelationsResponse> getTableRelations(
            @PathVariable Integer datasourceId,
            @PathVariable String tableName,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new RelationsResponse(
                        schemaReadService.getTableRelations(datasourceId, authenticatedUser.userId(), tableName)
                ));
    }
}
