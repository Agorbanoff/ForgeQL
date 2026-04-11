package com.example.core.postgres.api.controller;

import com.example.auth.filter.AuthenticatedUser;
import com.example.core.postgres.api.dto.request.AggregateRequest;
import com.example.core.postgres.api.dto.response.AggregateResponse;
import com.example.core.postgres.execution.AggregateQueryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/core/datasources/{datasourceId}/tables")
public class AggregateController {

    private final AggregateQueryService aggregateQueryService;

    public AggregateController(AggregateQueryService aggregateQueryService) {
        this.aggregateQueryService = aggregateQueryService;
    }

    @PostMapping("/{tableName:.+}/aggregate")
    public ResponseEntity<AggregateResponse> aggregate(
            @PathVariable Integer datasourceId,
            @PathVariable String tableName,
            @Valid @RequestBody AggregateRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(aggregateQueryService.aggregate(
                        datasourceId,
                        authenticatedUser.userId(),
                        tableName,
                        request
                ));
    }
}

