package com.example.controller.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserSignUpDTO(
        @NotBlank(message = "Username is required")
        @Size(min = 1, max = 20, message = "Username must be between 1 and 20 characters long")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email address")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters long")
        String password
) {
}
