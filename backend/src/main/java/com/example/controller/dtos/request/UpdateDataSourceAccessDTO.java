package com.example.controller.dtos.request;

import com.example.persistence.Enums.DataSourceAccessRole;
import jakarta.validation.constraints.NotNull;

public record UpdateDataSourceAccessDTO(
        @NotNull(message = "accessRole is required")
        DataSourceAccessRole accessRole
) {
}
