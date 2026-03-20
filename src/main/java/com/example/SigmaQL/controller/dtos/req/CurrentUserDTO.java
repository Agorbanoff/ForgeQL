package com.example.SigmaQL.controller.dtos.req;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CurrentUserDTO {
    private Integer id;
    private String email;
}