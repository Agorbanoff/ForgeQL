package com.example.SigmaQL.ast;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
public class QueryAST {

    public QueryAST queryAST(){
        return new QueryAST();
    }
}
