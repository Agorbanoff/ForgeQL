package com.example.controller.dtos.request;

import com.example.persistence.Enums.DataSourceAccessRole;
import jakarta.validation.constraints.NotNull;

public record AssignDataSourceAccessDTO(
        @NotNull(message = "userId is required")
        Integer userId,

        @NotNull(message = "accessRole is required")
        DataSourceAccessRole accessRole
) {
}
