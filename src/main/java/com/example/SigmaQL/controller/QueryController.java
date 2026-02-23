package com.example.SigmaQL.controller;

import com.example.SigmaQL.dtos.req.QueryReqDTO;
import com.example.SigmaQL.dtos.res.QueryResDTO;
import com.example.SigmaQL.service.QueryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class QueryController  {

    private final QueryService queryService;

    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }

   // @PostMapping("/query")
    //public ResponseEntity<QueryResDTO> Query(@RequestBody QueryReqDTO queryReqDTO) {
        //return ResponseEntity
                //.status(HttpStatus.BAD_REQUEST)
                //.body(queryService.executeQuery(query));
   // }
}
