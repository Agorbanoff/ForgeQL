package com.example.controller;

import com.example.auth.filter.JwtAuthenticationFilter;
import com.example.controller.dtos.request.ReqDataSourceDTO;
import com.example.controller.dtos.response.ResDataSourceDTO;
import com.example.service.DataSourceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/datasource")
public class DataSourceController {
    private final DataSourceService dataSourceService;
    private final JwtAuthenticationFilter  jwtAuthenticationFilter;

    @Autowired
    public DataSourceController(DataSourceService dataSourceService,
                                JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.dataSourceService = dataSourceService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResDataSourceDTO> getOneDataSource(@PathVariable Integer id) {
        Integer userId = jwtAuthenticationFilter.getCurrentUserId();

        ResDataSourceDTO resDataSourceDTO = dataSourceService.getDataSource(id, userId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(resDataSourceDTO);
    }

    @GetMapping
    public ResponseEntity<List<ResDataSourceDTO>> getAllDataSources() {
        Integer userId = jwtAuthenticationFilter.getCurrentUserId();

        List<ResDataSourceDTO> resDataSourceDTOList = dataSourceService.getAllDataSource(userId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(resDataSourceDTOList);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> saveDataSource(@Valid @RequestBody ReqDataSourceDTO reqDataSourceDTO) {
        Integer userId = jwtAuthenticationFilter.getCurrentUserId();

        dataSourceService.saveDataSource(reqDataSourceDTO, userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .build();
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateDataSource(@Valid @RequestBody ReqDataSourceDTO reqDataSourceDTO,
                                                 @PathVariable Integer id) {

        Integer userId = jwtAuthenticationFilter.getCurrentUserId();

        dataSourceService.updateDataSource(reqDataSourceDTO, userId, id);

        return ResponseEntity
                .status(HttpStatus.OK)
                .build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDataSource(@PathVariable Integer id) {
        Integer userId = jwtAuthenticationFilter.getCurrentUserId();

        dataSourceService.deleteDataSource(id, userId);

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }
}