package com.example.controller.dtos.response;

public record CurrentUserDTO(
        Integer id,
        String email,
        String username
) {
}
