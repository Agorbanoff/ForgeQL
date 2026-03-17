package com.example.SigmaQL.controller;

import com.example.SigmaQL.dtos.req.QueryReqDTO;
import com.example.SigmaQL.service.QueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class QueryController {

    private final QueryService queryService;

    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }

    @PostMapping("/query")
    public ResponseEntity<List<Map<String, Object>>> query(@RequestBody QueryReqDTO queryReqDTO) throws Exception {
        return ResponseEntity.ok(queryService.execute(queryReqDTO));
    }
}