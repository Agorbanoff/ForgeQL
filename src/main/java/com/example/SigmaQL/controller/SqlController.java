package com.example.SigmaQL.controller;

import com.example.SigmaQL.common.exceptions.InvalidQueryException;
import com.example.SigmaQL.common.exceptions.UnknownFieldException;
import com.example.SigmaQL.controller.dtos.request.QueryReqDTO;
import com.example.SigmaQL.parser.QueryValidator;
import com.example.SigmaQL.service.QueryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/query")
@CrossOrigin(origins = "http://localhost:5173")
public class SqlController {

    private final QueryValidator queryValidator;
    private final QueryService queryService;

    public SqlController(QueryValidator queryValidator, QueryService queryService) {
        this.queryValidator = queryValidator;
        this.queryService = queryService;
    }

    @PostMapping
    public ResponseEntity<List<Map<String, Object>>> query(@RequestBody QueryReqDTO dto) throws InvalidQueryException, UnknownFieldException {
        queryValidator.validate(dto);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(queryService.execute(dto));
    }
}
