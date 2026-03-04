package com.example.SigmaQL.controller;

import com.example.SigmaQL.common.exceptions.InvalidQueryException;
import com.example.SigmaQL.common.exceptions.UnknownFieldException;
import com.example.SigmaQL.dtos.req.QueryReqDTO;
import com.example.SigmaQL.parser.QueryValidator;
import com.example.SigmaQL.service.QueryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/query")
public class SqlController {

    private final QueryValidator validator;
    private final QueryService service;

    public SqlController(QueryValidator validator, QueryService service) {
        this.validator = validator;
        this.service = service;
    }

    @PostMapping
    public List<Map<String, Object>> query(@RequestBody QueryReqDTO dto) throws InvalidQueryException, UnknownFieldException {
        validator.validate(dto);
        return service.execute(dto);
    }
}