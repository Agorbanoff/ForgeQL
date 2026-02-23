package com.example.SigmaQL.dtos.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncludeReqDTO {
    private List<String> fields;

    private Map<String, Map<String, Object>> filter;

    private Map<String, IncludeReqDTO> include;

    private Integer limit;
    private Integer offset;

    private List<OrderByReqDTO> orderBy;
}