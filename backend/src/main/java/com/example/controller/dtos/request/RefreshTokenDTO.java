package com.example.controller.dtos.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenDTO(
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {
}
