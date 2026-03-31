package com.example.auth.filter;

public record AuthenticatedUser(
        Integer userId,
        String email
) {
}