package com.example.SigmaQL.controller.dtos.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CurrentUserDTO {
    private Integer id;
    private String email;
}