package com.example.SigmaQL.auth.filter;

public record AuthenticatedUser(
        Integer userId,
        String email
) {
}