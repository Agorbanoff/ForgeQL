package com.example.SigmaQL.service;

import com.example.SigmaQL.parser.FilterParser;
import com.example.SigmaQL.parser.IncludeParser;
import com.example.SigmaQL.parser.QueryParser;
import com.example.SigmaQL.parser.QueryValidator;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class QueryService {

    private final FilterParser filterParser;
    private final IncludeParser includeParser;
    private final QueryParser queryParser;
    private final QueryValidator queryValidator;

    public QueryService(FilterParser filterParser, IncludeParser includeParser, QueryParser queryParser, QueryValidator queryValidator) {
        this.filterParser = filterParser;
        this.includeParser = includeParser;
         this.queryParser = queryParser;
        this.queryValidator = queryValidator;
    }

    public Map<String, Object> executeQuery(Map<String, Object> query) {
        filterParser.filterParser(query);
        includeParser.includeParser(query);
        queryParser.queryParser(query);
        queryValidator.queryValidator(query);
        return query;
    }
}