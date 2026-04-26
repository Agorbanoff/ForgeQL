package com.example.controller.dtos.response;

import com.example.persistence.Enums.GlobalRole;

import java.time.Instant;

public record AdminUserDTO(
        Integer id,
        String username,
        String email,
        GlobalRole globalRole,
        Instant createdAt
) {
}
