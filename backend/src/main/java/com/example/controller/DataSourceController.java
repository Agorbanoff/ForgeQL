package com.example.controller;

import com.example.auth.filter.AuthenticatedUser;
import com.example.controller.dtos.request.ReqDataSourceDTO;
import com.example.controller.dtos.request.UpdateDataSourceDTO;
import com.example.controller.dtos.response.ResDataSourceConnectionTestDTO;
import com.example.controller.dtos.response.ResDataSourceDTO;
import com.example.service.DataSourceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/datasource")
public class DataSourceController {
    private final DataSourceService dataSourceService;

    public DataSourceController(DataSourceService dataSourceService) {
        this.dataSourceService = dataSourceService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResDataSourceDTO> getOneDataSource(
            @PathVariable Integer id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        ResDataSourceDTO resDataSourceDTO = dataSourceService.getDataSource(id, authenticatedUser.userId());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(resDataSourceDTO);
    }

    @GetMapping
    public ResponseEntity<List<ResDataSourceDTO>> getAllDataSources(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        List<ResDataSourceDTO> resDataSourceDTOList = dataSourceService.getAllDataSource(authenticatedUser.userId());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(resDataSourceDTOList);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> saveDataSource(
            @Valid @RequestBody ReqDataSourceDTO reqDataSourceDTO,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        dataSourceService.saveDataSource(reqDataSourceDTO, authenticatedUser.userId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .build();
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateDataSource(
            @Valid @RequestBody UpdateDataSourceDTO updateDataSourceDTO,
            @PathVariable Integer id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        dataSourceService.updateDataSource(updateDataSourceDTO, authenticatedUser.userId(), id);

        return ResponseEntity
                .status(HttpStatus.OK)
                .build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDataSource(
            @PathVariable Integer id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        dataSourceService.deleteDataSource(id, authenticatedUser.userId());

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    @PostMapping("/{id}/test-connection")
    public ResponseEntity<ResDataSourceConnectionTestDTO> testDataSourceConnection(
            @PathVariable Integer id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        ResDataSourceConnectionTestDTO response = dataSourceService.testDataSourceConnection(
                id,
                authenticatedUser.userId()
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }
}
