package com.example.SigmaQL.controller.dtos.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenDTO {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}