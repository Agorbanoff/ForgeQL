package com.example.SigmaQL.dtos.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderByReqDTO {
    private String field;
    private String direction;
}   