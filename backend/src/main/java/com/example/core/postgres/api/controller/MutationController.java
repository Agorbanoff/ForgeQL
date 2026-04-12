package com.example.core.postgres.api.controller;

import com.example.auth.filter.AuthenticatedUser;
import com.example.core.postgres.api.dto.request.CreateRowRequest;
import com.example.core.postgres.api.dto.request.UpdateRowRequest;
import com.example.core.postgres.api.dto.response.CreateRowResponse;
import com.example.core.postgres.api.dto.response.DeleteRowResponse;
import com.example.core.postgres.api.dto.response.UpdateRowResponse;
import com.example.core.postgres.execution.MutationRowsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/core/datasources/{datasourceId}/tables")
public class MutationController {

    private final MutationRowsService mutationRowsService;

    public MutationController(
            MutationRowsService mutationRowsService
    ) {
        this.mutationRowsService = mutationRowsService;
    }

    @PostMapping("/{tableName:.+}/rows")
    public ResponseEntity<CreateRowResponse> createRow(
            @PathVariable Integer datasourceId,
            @PathVariable String tableName,
            @Valid @RequestBody CreateRowRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(mutationRowsService.createRow(
                        datasourceId,
                        authenticatedUser.userId(),
                        tableName,
                        request
                ));
    }

    @PatchMapping("/{tableName:.+}/rows/{id}")
    public ResponseEntity<UpdateRowResponse> updateRow(
            @PathVariable Integer datasourceId,
            @PathVariable String tableName,
            @PathVariable Long id,
            @Valid @RequestBody UpdateRowRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(mutationRowsService.updateRow(
                        datasourceId,
                        authenticatedUser.userId(),
                        tableName,
                        id,
                        request
                ));
    }

    @DeleteMapping("/{tableName:.+}/rows/{id}")
    public ResponseEntity<DeleteRowResponse> deleteRow(
            @PathVariable Integer datasourceId,
            @PathVariable String tableName,
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(mutationRowsService.deleteRow(
                        datasourceId,
                        authenticatedUser.userId(),
                        tableName,
                        id
                ));
    }
}

