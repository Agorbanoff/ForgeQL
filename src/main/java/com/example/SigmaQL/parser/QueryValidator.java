package com.example.SigmaQL.parser;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class QueryValidator {


    public Map<String, Object> queryValidator(Map<String, Object> query){
        return query;
    }
}
