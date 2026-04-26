package com.example.controller.dtos.response;

import com.example.persistence.Enums.DataSourceAccessRole;

import java.time.Instant;

public record DataSourceAccessDTO(
        Integer userId,
        String username,
        String email,
        DataSourceAccessRole accessRole,
        Instant createdAt
) {
}
