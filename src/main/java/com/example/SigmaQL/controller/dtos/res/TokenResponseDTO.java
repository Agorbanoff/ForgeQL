package com.example.SigmaQL.controller.dtos.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponseDTO {
    private String type;
    private String accessToken;
    private String refreshToken;
}