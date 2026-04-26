package com.example.controller.dtos.response;

import com.example.persistence.Enums.DataSourceAccessRole;
import com.example.persistence.Enums.GlobalRole;

import java.time.Instant;

public record DataSourceAccessDTO(
        Integer userId,
        String username,
        String email,
        GlobalRole globalRole,
        DataSourceAccessRole accessRole,
        Instant createdAt
) {
}
