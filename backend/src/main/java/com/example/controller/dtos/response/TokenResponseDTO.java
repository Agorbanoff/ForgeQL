package com.example.controller.dtos.response;

public record TokenResponseDTO(
        String type,
        String accessToken,
        String refreshToken
) {
}
